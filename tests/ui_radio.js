// Checks the three-tab page against a stub of the Android bridge. Writes ui_record.png, ui_live.png, ui_files.png.
// Run: node tests/ui_radio.js
const { chromium } = require('playwright'); const path = require('path');
const STUB = `window.__c=[];window.__radio={on:false,op:'',talk:false,sent:0,frames:0,got:0,needed:0,sync:false,snr:0,level:0,left:0};
window.Android=new Proxy({},{get:(t,k)=>(...a)=>{__c.push(k+':'+a.join(','));
 if(k=='microphones')return JSON.stringify([{id:7,name:'Phone microphone (bottom)'},{id:9,name:'USB'}]);
 if(k=='list')return JSON.stringify([{name:'2026-11-03_16-10-00_700C.c2',mode:'700C',secs:30,bytes:2632,time:1},{name:'long_700C.c2',mode:'700C',secs:300,bytes:26257,time:0}]);
 if(k=='rename')return true;
 if(k=='status')return JSON.stringify({rec:false,paused:false,waiting:false,vad:.3,gain:12,level:.4,secs:0,bytes:0,play:null,pos:0,dur:0,msg:null,sampling:false,hasNoise:true,comp:-1,clip:false,routed:'',radio:window.__radio});
}})`;
(async () => {
  const fails = []; const check = (ok, what) => { if (!ok) { fails.push(what); console.log('FAIL', what); } };
  const b = await chromium.launch(); const pg = await b.newPage({ viewport: { width: 393, height: 800 }, deviceScaleFactor: 2 });
  const errs = []; pg.on('pageerror', e => errs.push(String(e)));
  await pg.addInitScript(STUB);
  await pg.goto('file://' + path.resolve(__dirname, '..', 'app', 'assets', 'index.html'));
  const calls = () => pg.evaluate('__c'), vis = s => pg.isVisible(s), set = o => pg.evaluate(o), wait = () => pg.waitForTimeout(450);
  await wait();
  // Record tab: lean by default, settings fold out, recording still starts with the same calls
  check(await vis('#recb') && !await vis('#mic') && !await vis('#live') && !await vis('#files'), 'record tab default');
  await pg.screenshot({ path: 'ui_record.png' });
  await pg.click('#adv'); check(await vis('#mic') && await vis('#src') && await vis('#samp') && await vis('#st'), 'settings unfold');
  check(await pg.inputValue('#src') === '6', 'default source is VOICE_RECOGNITION');
  await pg.screenshot({ path: 'ui_settings.png' });
  await pg.click('#adv'); await pg.click('#recb');
  let c = await calls(); check(c.some(x => x.startsWith('startRec:700C')), 'startRec'); check(c.includes('audioConfig:6,0,1,0.18,180,6,12,3'), 'audioConfig defaults unchanged: ' + c.filter(x => x.startsWith('audioConfig')).slice(-1));
  await pg.click('#list .a button:has-text("MORE")'); check(await vis('button:has-text("SAVE WAV")'), 'more menu');
  // Files tab
  await pg.click('#tabs button[data-t=files]'); await pg.waitForTimeout(100);
  check(await vis('#frx') && !await vis('#recb') && !await vis('#cmp'), 'files tab');
  let t = await pg.innerText('#list'); check(t.includes('SEND 01:59'), 'send time, 30 s recording, robust: ' + t.replace(/\n/g, ' ').slice(0, 120));
  await pg.selectOption('#rmode', '0'); t = await pg.innerText('#list'); check(t.includes('SEND 00:52'), 'send time fast: ' + t.replace(/\n/g, ' ').slice(0, 120));
  await pg.selectOption('#rmode', '1'); await pg.selectOption('#rex', '100');
  await pg.click('#list .a button:has-text("SEND")'); c = await calls(); check(c.includes('radioSend:2026-11-03_16-10-00_700C.c2,1,100,false'), 'radioSend ' + c.slice(-3));
  await pg.selectOption('#rex', '-1'); await pg.click('#list .a button:has-text("SEND")'); c = await calls(); check(c.includes('radioSend:2026-11-03_16-10-00_700C.c2,1,0,true'), 'radioSend repeat ' + c.slice(-3));
  await pg.selectOption('#rex', '50');
  await pg.click('#fany'); check((await calls()).includes('radioSendFile:1,50,false'), 'radioSendFile ' + (await calls()).slice(-2)); check((await pg.innerText('#rate')).includes('1.4 kB per minute'), 'rate text: ' + await pg.innerText('#rate'));
  await pg.click('#frx'); check((await calls()).includes('radioReceive:1'), 'radioReceive');
  await set("Object.assign(__radio,{on:true,op:'receive',got:9,needed:22,sync:true,snr:6.2,level:.5})"); await wait();
  check((await pg.innerText('#fst')).includes('RECEIVING') && (await pg.innerText('#fsig')).includes('Got 9 of 22') && await vis('#fstop') && !await vis('#frx') && !await vis('#fany'), 'receiving view');
  await pg.screenshot({ path: 'ui_files.png' });
  await pg.click('#fstop'); check((await calls()).includes('radioStop:'), 'radioStop');
  await set("Object.assign(__radio,{on:true,op:'send',sent:12,frames:36,left:80})"); await wait();
  check((await pg.innerText('#fsig')).includes('Frame 12 of 36'), 'sending view');
  await set("Object.assign(__radio,{on:false,op:''})"); await wait();
  check((await pg.innerText('#fst')).includes('READY') && await vis('#frx'), 'ready again');
  // Live tab
  await pg.click('#tabs button[data-t=live]'); await pg.waitForTimeout(100);
  check(await vis('#ptt') && await pg.isDisabled('#ptt') && !await vis('#list'), 'live tab off');
  await pg.click('#lon'); check((await calls()).includes('radioLive:10,false,true'), 'radioLive ' + (await calls()).slice(-2));
  await set("Object.assign(__radio,{on:true,op:'live',talk:false,sync:true,snr:4,level:.3})"); await wait();
  check((await pg.innerText('#lst')).includes('LISTENING') && !await pg.isDisabled('#ptt'), 'live listening');
  await pg.click('#ptt'); check((await calls()).includes('radioTalk:true'), 'talk');
  await set('__radio.talk=true'); await wait();
  check((await pg.innerText('#lst')).includes('TALKING'), 'live talking'); await pg.screenshot({ path: 'ui_live.png' });
  await pg.click('#ptt'); check((await calls()).includes('radioTalk:false'), 'listen again');
  for (const w of [320, 393]) { // nothing may stick out sideways on a narrow phone
    await pg.setViewportSize({ width: w, height: 700 });
    for (const tb of ['deck', 'live', 'files']) {
      await pg.click(`#tabs button[data-t=${tb}]`);
      check(await pg.evaluate("document.documentElement.scrollWidth<=innerWidth && [...document.querySelectorAll('.deck:not([hidden]) *')].every(e=>e.getBoundingClientRect().right<=innerWidth+1)"), `fits width ${w} ${tb}`);
    }
  }
  check(!errs.length, 'page errors ' + errs);
  await b.close();
  console.log(fails.length ? fails.length + ' UI FAILURES' : 'UI OK'); process.exit(fails.length ? 1 : 0);
})();
