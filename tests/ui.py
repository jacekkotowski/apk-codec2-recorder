import os
import asyncio
from playwright.async_api import async_playwright
STUB="""window.__c=[];window.Android=new Proxy({},{get:(t,k)=>(...a)=>{__c.push(k+':'+a.join(','));
 if(k=='microphones')return JSON.stringify([{id:7,name:'Phone microphone (bottom)'},{id:9,name:'USB'}]);
 if(k=='list')return JSON.stringify([{name:'2026-11-03_16-10-00_700C.c2',mode:'700C',secs:64,bytes:6407,time:1}]);
 if(k=='status')return JSON.stringify({rec:window.__rec||false,paused:false,waiting:true,vad:.3,gain:12,level:.4,secs:12,bytes:1200,play:null,pos:0,dur:0,msg:null,sampling:false,hasNoise:true});
}})"""
async def main():
  async with async_playwright() as p:
    b=await p.chromium.launch();pg=await b.new_page(viewport={'width':393,'height':800},device_scale_factor=2)
    errs=[];pg.on('pageerror',lambda e:errs.append(str(e)))
    await pg.add_init_script(STUB);await pg.goto('file://'+os.path.abspath(os.path.join(os.path.dirname(__file__),'..','app','assets','index.html'))+'')
    await pg.check('#va');await pg.evaluate("window.__rec=true");await pg.wait_for_timeout(500)
    await pg.screenshot(path='ui.png')
    await pg.evaluate("window.__rec=false");await pg.wait_for_timeout(300);await pg.click('#recb');await pg.wait_for_timeout(100)
    print([c for c in await pg.evaluate("__c") if c.startswith(('startRec','audioConfig'))][-2:],'errs',errs,'deckH',await pg.evaluate("document.getElementById('deck').offsetHeight"),'listH',await pg.evaluate("document.getElementById('list').offsetHeight"))
    await b.close()
asyncio.run(main())
