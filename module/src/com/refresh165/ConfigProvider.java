package com.refresh165;

import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;

/** Public read-only config; writes are restricted to this APK or platform Settings. */
public final class ConfigProvider extends ContentProvider {
    private android.content.SharedPreferences prefs;
    @Override public boolean onCreate(){prefs=getContext().createDeviceProtectedStorageContext().getSharedPreferences("display",0);return true;}
    @Override public synchronized Bundle call(String method,String arg,Bundle extras){
        if("read".equals(method)){
            Bundle b=new Bundle();for(String k:Config.KEYS)b.putBoolean(k,prefs.getBoolean(k,Config.defaultOn(k)));
            b.putInt("rate",Config.validRate(prefs.getInt("rate",165)));
            b.putInt("dark",prefs.getInt("dark",80));b.putInt("bright",prefs.getInt("bright",160));b.putInt("multiplier",prefs.getInt("multiplier",160));
            b.putInt("fixed",Math.max(2,Math.min(255,prefs.getInt("fixed",80))));
            b.putString("app_profiles",prefs.getString("app_profiles",""));
            b.putString("apps",prefs.getString("apps",""));return b;
        }
        if((!"write".equals(method)&&!"app_profile".equals(method)&&!"reset_profiles".equals(method))||extras==null)throw new IllegalArgumentException("Unsupported call");
        int uid=Binder.getCallingUid();boolean owner=uid==android.os.Process.myUid();
        boolean settings=false;
        try{PackageManager pm=getContext().getPackageManager();settings=pm.getPackageUid("com.android.settings",0)==uid&&pm.checkSignatures("android","com.android.settings")==PackageManager.SIGNATURE_MATCH;}catch(Exception ignored){}
        if(!owner&&!settings)throw new SecurityException("Configuration is writable only by its owner / platform Settings");
        android.content.SharedPreferences.Editor e=prefs.edit();
        if("app_profile".equals(method)){String encoded=AppProfiles.update(prefs.getString("app_profiles",""),extras.getString("packages",""),extras.getString("policy",""));e.putString("app_profiles",encoded);e.putBoolean("app_policies",true);if(!e.commit())throw new IllegalStateException("Cannot save profiles");getContext().getContentResolver().notifyChange(Config.URI,null);return call("read",null,null);}
        if("reset_profiles".equals(method)){e.putString("app_profiles","");e.putString("apps","");if(!e.commit())throw new IllegalStateException("Cannot reset profiles");getContext().getContentResolver().notifyChange(Config.URI,null);return call("read",null,null);}

        if(owner)for(String k:Config.KEYS)if(extras.containsKey(k))e.putBoolean(k,extras.getBoolean(k));
        if(extras.containsKey("rate"))e.putInt("rate",Config.validRate(extras.getInt("rate")));
        if(owner){for(String k:new String[]{"dark","bright","multiplier","fixed"})if(extras.containsKey(k))e.putInt(k,Math.max(k.equals("multiplier")?25:k.equals("fixed")?2:1,Math.min(k.equals("multiplier")?300:255,extras.getInt(k))));
            if(extras.containsKey("apps")){String s=extras.getString("apps","");if(s.length()>200000)throw new IllegalArgumentException("Too many packages");e.putString("apps",s);}}
        if(!e.commit())throw new IllegalStateException("Cannot save configuration");
        getContext().getContentResolver().notifyChange(Config.URI,null);return Bundle.EMPTY;
    }
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String o){return null;}
    @Override public String getType(Uri u){return "vnd.android.cursor.item/vnd.refresh165.config";}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
