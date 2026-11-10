"""Reproducible Windows SDK build. Requires JDK17+, Python3, Android SDK34+.
Usage: python build_windows.py --android-jar PATH --build-tools PATH [--out PATH]
Self-signed test APK; the local key is never included in source exports.
"""
import argparse, hashlib, os, subprocess, zipfile
from pathlib import Path
import xml.etree.ElementTree as ET

def run(*args):
    subprocess.run([str(x) for x in args], check=True)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--android-jar',required=True,type=Path)
    ap.add_argument('--build-tools',required=True,type=Path)
    ap.add_argument('--out',type=Path)
    a=ap.parse_args()
    root=Path(__file__).resolve().parent
    out=(a.out or root/'out-windows').resolve()
    if (out/'classes').exists():
        raise SystemExit('Use a fresh --out directory; build never recursively deletes files.')
    for name in ['classes','dex','apk']: (out/name).mkdir(parents=True,exist_ok=True)
    android=a.android_jar.resolve();bt=a.build_tools.resolve();mod=root/'module'
    (out/'sdk-sha256.txt').write_text(hashlib.sha256(android.read_bytes()).hexdigest()+'  android.jar\n')
    values=dict(line.split('=',1) for line in (root/'version.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
    sources=sorted((mod/'src').rglob('*.java'))
    (out/'sources.txt').write_text('\n'.join('"'+p.as_posix()+'"' for p in sources),encoding='utf-8')
    run('javac','--release','11','-encoding','UTF-8','-cp',str(android)+os.pathsep+str(root/'libs/libxposed-api-102.jar'),'-d',out/'classes','@'+str(out/'sources.txt'))
    with zipfile.ZipFile(out/'classes.jar','w',zipfile.ZIP_STORED) as classes:
        for f in (out/'classes').rglob('*.class'):classes.write(f,f.relative_to(out/'classes').as_posix())
    # Invoke the Java tools directly; avoids cmd argument re-parsing in .bat wrappers.
    run('java','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--release','--min-api','29','--lib',android,'--output',out/'dex',out/'classes.jar')
    run(bt/'aapt2.exe','compile','--dir',mod/'res','-o',out/'res.zip')
    ns='http://schemas.android.com/apk/res/android';ET.register_namespace('android',ns)
    manifest=ET.parse(mod/'AndroidManifest.xml')
    manifest.getroot().set('{'+ns+'}versionCode',values['versionCode'])
    manifest.getroot().set('{'+ns+'}versionName',values['versionName'])
    manifest.write(out/'AndroidManifest.xml',encoding='utf-8',xml_declaration=True)
    run(bt/'aapt2.exe','link','-o',out/'apk/base.apk','-I',android,'--manifest',out/'AndroidManifest.xml',out/'res.zip')
    with zipfile.ZipFile(out/'apk/base.apk') as base,zipfile.ZipFile(out/'apk/unsigned.apk','w',zipfile.ZIP_STORED) as dst:
        for i in base.infolist():dst.writestr(i,base.read(i.filename))
        dst.write(out/'dex/classes.dex','classes.dex')
        for f in (mod/'assets').rglob('*'):
            if not f.is_file():continue
            rel=f.relative_to(mod/'assets').as_posix()
            dst.write(f,'assets/'+rel)
            if rel.startswith('META-INF/'):dst.write(f,rel)
    run(bt/'zipalign.exe','-f','-p','4',out/'apk/unsigned.apk',out/'apk/aligned.apk')
    key=root/'keystore-op15.jks'
    if not key.exists():
        run('keytool','-genkeypair','-keystore',key,'-alias','op15','-keyalg','RSA','-keysize','2048','-validity','3650',
            '-storepass','op15test','-keypass','op15test','-dname','CN=LTPO-X (COS) OP15 Experimental')
    apk=out/('LTPO-X-COS-'+values['versionName']+'.apk')
    run('java','-jar',bt/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias','op15','--ks-pass','pass:op15test','--key-pass','pass:op15test','--out',apk,out/'apk/aligned.apk')
    run('java','-jar',bt/'lib/apksigner.jar','verify','--verbose','--print-certs',apk)
    run(bt/'zipalign.exe','-c','-p','4',apk)
    print(hashlib.sha256(apk.read_bytes()).hexdigest(),apk.name)

if __name__=='__main__':main()
