"""Read-only FDT/DTBO inspection; never writes firmware or sends panel commands."""
from pathlib import Path
import argparse,struct,json,hashlib,re
def parse(data):
 h=struct.unpack_from('>10I',data);assert h[0]==0xd00dfeed
 strings=data[h[3]:h[3]+h[8]];pos=h[2];limit=pos+h[9];stack=[];nodes={}
 while pos<limit:
  token=struct.unpack_from('>I',data,pos)[0];pos+=4
  if token==1:
   end=data.index(0,pos);stack.append(data[pos:end].decode());pos=(end+4)&~3;nodes['/'+('/'.join(stack).lstrip('/'))]={}
  elif token==2:stack.pop()
  elif token==3:
   length,nameoff=struct.unpack_from('>II',data,pos);pos+=8;end=strings.index(0,nameoff);name=strings[nameoff:end].decode();nodes['/'+('/'.join(stack).lstrip('/'))][name]=data[pos:pos+length];pos=(pos+length+3)&~3
  elif token==4:pass
  elif token==9:break
  else:raise ValueError(token)
 return nodes
def ints(value):return list(struct.unpack('>'+str(len(value)//4)+'I',value)) if value else []

def audit(nodes,panel):
 rows=[]
 for path,props in nodes.items():
  if '/'+panel+'/' not in path or '__local_fixups__' in path:continue
  rate=props.get('qcom,mdss-dsi-panel-framerate')
  if not rate:continue
  families={f:sum(bool(re.fullmatch('qcom,mdss-dsi-'+f+'adfr-min-fps-[0-9]+-command',k)) for k in props) for f in ['','hpwm-','bigdc-']}
  rows.append({'path':path,'physical_hz':ints(rate)[0],'mapping':ints(props.get('oplus,adfr-min-fps-mapping-table',b'')),
    'command_counts':families,'properties':{k:{'bytes':len(v),'sha256':hashlib.sha256(v).hexdigest()} for k,v in sorted(props.items()) if 'adfr' in k}})
 return rows

def main():
 a=argparse.ArgumentParser(description=__doc__)
 a.add_argument('--live-fdt',type=Path,required=True);a.add_argument('--dtbo',type=Path)
 a.add_argument('--index',type=int,default=10)
 a.add_argument('--panel',default='qcom,mdss_dsi_panel_AD296_P_3_A0020_dsc_cmd')
 a.add_argument('--out',type=Path,required=True);args=a.parse_args()
 b=args.live_fdt.read_bytes();result={'panel':args.panel,'live_fdt_sha256':hashlib.sha256(b).hexdigest(),'live':audit(parse(b),args.panel),'partition_writes':False}
 if args.dtbo:
  b=args.dtbo.read_bytes();h=struct.unpack_from('>8I',b)
  if h[0]!=0xd7b7ab1e or not 0<=args.index<h[4]:raise ValueError('Invalid DTBO/index')
  e=struct.unpack_from('>8I',b,h[5]+args.index*h[3]);dtb=b[e[1]:e[1]+e[0]]
  result.update(dtbo_sha256=hashlib.sha256(b).hexdigest(),overlay_index=args.index,overlay=audit(parse(dtb),args.panel))
  normalized=lambda rows:{row['path'].rsplit('/',1)[1]:{k:v for k,v in row.items() if k!='path'} for row in rows}
  assert normalized(result['live'])==normalized(result['overlay']),'Live/selected-overlay properties differ; review before any candidate'
 args.out.parent.mkdir(parents=True,exist_ok=True);args.out.write_text(json.dumps(result,indent=2),encoding='utf-8')
 for row in result['live']:print(row['physical_hz'],row['mapping'],row['command_counts'])
if __name__=='__main__':main()
