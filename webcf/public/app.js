/* Ayham Player Web — نسخة الآيفون السينمائية (PWA). كل بيانات الاشتراك على الخادم؛ المتصفح لا يراها. */
(()=>{
'use strict';
const $=(s,r=document)=>r.querySelector(s);
const $$=(s,r=document)=>Array.from(r.querySelectorAll(s));
const esc=s=>String(s==null?'':s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const store={
  get(k,d){try{const v=localStorage.getItem('ap_'+k);return v?JSON.parse(v):d}catch(e){return d}},
  set(k,v){try{localStorage.setItem('ap_'+k,JSON.stringify(v))}catch(e){}},
};
const now=()=>Date.now();
const sleep=ms=>new Promise(r=>setTimeout(r,ms));

/* ================= اللغة ================= */
const L={
ar:{home:'الرئيسية',movies:'الأفلام',series:'المسلسلات',live:'مباشر',mylist:'قائمتي',search:'بحث',play:'تشغيل',resume:'متابعة',details:'التفاصيل',
 cont:'تابع المشاهدة',top10m:'أفضل 10 أفلام',top10s:'أفضل 10 مسلسلات',newm:'أُضيفت حديثًا · أفلام',news:'أُضيفت حديثًا · مسلسلات',all:'الكل',more:'عرض المزيد',
 loading:'جارٍ التحميل…',retry:'إعادة المحاولة',empty:'لا توجد عناصر.',newest:'الأحدث',rated:'الأعلى تقييمًا',az:'أ - ي',season:'الموسم',ep:'حلقة',eps:'حلقات',
 cast:'طاقم التمثيل',similar:'مشابه لما شاهدت',read:'اقرأ المزيد',less:'أقل',addl:'أضف لقائمتي',inl:'في قائمتي',watched:'تمت مشاهدتها',upnext:'التالية',cres:'أكمل',
 login:'دخول',code:'كلمة المرور',tag:'كل ما تحب مشاهدته في مكان واحد',enter:'اكتب كلمة المرور.',
 menu:'الإعدادات',lang:'اللغة',logout:'تسجيل الخروج',refresh:'تحديث القوائم',sub:'الاشتراك',active:'فعّال',exp:'ينتهي',conn:'اتصالات',
 srch_ph:'ابحث عن فيلم أو مسلسل أو قناة',srch_min:'اكتب حرفين على الأقل.',srch_none:'لا توجد نتائج.',searching:'جارٍ البحث…',
 subs:'الترجمة',off:'إيقاف',file:'اختيار ملف ترجمة…',delay:'تأخير الترجمة',audio:'الصوت',speed:'السرعة',pip:'نافذة',air:'AirPlay',fs:'ملء الشاشة',
 nextep:'الحلقة التالية',live_:'مباشر',now:'الآن',next:'التالي',a2h:'ثبّت التطبيق: اضغط زر المشاركة ⎋ ثم «إضافة إلى الشاشة الرئيسية» لتجربة بدون أشرطة سفاري.',
 net:'لا يوجد اتصال بالإنترنت.',auth:'انتهت الجلسة. ادخل الكود من جديد.',cfg:'الموقع غير مهيّأ بعد.',
 unsupported:'صيغة هذا الملف لا يدعمها سفاري على الآيفون (مثل MKV أو صوت AC3/DTS). جرّب نسخة أخرى من نفس العمل إن وُجدت، أو شاهده من تطبيق أندرويد/التلفاز.',
 livefail:'تعذّر تشغيل هذه القناة الآن. جرّب قناة أخرى أو أعد المحاولة.',vidfail:'تعذّر تشغيل الفيديو.',
 trending:'🔥 رقم',today:'اليوم',min:'د',
},
en:{home:'Home',movies:'Movies',series:'Series',live:'Live',mylist:'My List',search:'Search',play:'Play',resume:'Resume',details:'Details',
 cont:'Continue Watching',top10m:'Top 10 Movies',top10s:'Top 10 Series',newm:'New · Movies',news:'New · Series',all:'All',more:'Show more',
 loading:'Loading…',retry:'Retry',empty:'Nothing here.',newest:'Newest',rated:'Top rated',az:'A - Z',season:'Season',ep:'ep',eps:'episodes',
 cast:'Cast',similar:'More like this',read:'Read more',less:'Less',addl:'Add to list',inl:'In my list',watched:'Watched',upnext:'Up next',cres:'Resume',
 login:'Sign in',code:'Password',tag:'Everything you love, in one place',enter:'Enter your password.',
 menu:'Settings',lang:'Language',logout:'Sign out',refresh:'Refresh lists',sub:'Subscription',active:'Active',exp:'Expires',conn:'Connections',
 srch_ph:'Search movies, series or channels',srch_min:'Type at least 2 letters.',srch_none:'No results.',searching:'Searching…',
 subs:'Subtitles',off:'Off',file:'Choose subtitle file…',delay:'Subtitle delay',audio:'Audio',speed:'Speed',pip:'PiP',air:'AirPlay',fs:'Fullscreen',
 nextep:'Next episode',live_:'LIVE',now:'Now',next:'Next',a2h:'Install the app: tap Share ⎋ then “Add to Home Screen” for a full-screen experience.',
 net:'No internet connection.',auth:'Session expired. Enter your code again.',cfg:'Site is not configured yet.',
 unsupported:'Safari on iPhone cannot play this file format (e.g. MKV or AC3/DTS audio). Try another version of the title, or watch on the Android/TV app.',
 livefail:'This channel cannot be played right now. Try another or retry.',vidfail:'Could not play the video.',
 trending:'🔥 #',today:'today',min:'m',
}};
let lang=store.get('lang','ar');
const t=k=>(L[lang]&&L[lang][k])||L.ar[k]||k;
function applyLang(){document.documentElement.lang=lang;document.documentElement.dir=lang==='ar'?'rtl':'ltr';$('#lbtn').textContent=t('login');$('#in-pwd').placeholder=t('code');$('#ltag').textContent=t('tag')}

/* ================= أيقونات ================= */
const I={
 home:'<svg viewBox="0 0 24 24"><path d="M3 11l9-8 9 8M5 10v10h5v-6h4v6h5V10"/></svg>',
 movie:'<svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="16" rx="2"/><path d="M3 9h18M8 4v5M16 4v5"/></svg>',
 series:'<svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="12" rx="2"/><path d="M8 21h8M12 17v4"/></svg>',
 live:'<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="2"/><path d="M7.8 7.8a6 6 0 0 0 0 8.4M16.2 7.8a6 6 0 0 1 0 8.4M4.9 4.9a10 10 0 0 0 0 14.2M19.1 4.9a10 10 0 0 1 0 14.2"/></svg>',
 list:'<svg viewBox="0 0 24 24"><path d="M12 21s-8-5.2-8-11a4.5 4.5 0 0 1 8-2.8A4.5 4.5 0 0 1 20 10c0 5.8-8 11-8 11z"/></svg>',
 search:'<svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M20 20l-4-4"/></svg>',
 menu:'<svg viewBox="0 0 24 24"><path d="M4 7h16M4 12h16M4 17h16"/></svg>',
 play:'<svg viewBox="0 0 24 24"><path d="M7 4.5v15l13-7.5z"/></svg>',
 pause:'<svg viewBox="0 0 24 24"><path d="M6 4h4v16H6zM14 4h4v16h-4z"/></svg>',
 plus:'<svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg>',
 check:'<svg viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>',
 info:'<svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5v.5"/></svg>',
 back:'<svg viewBox="0 0 24 24"><path d="M9 5l7 7-7 7"/></svg>',
 close:'<svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>',
 cc:'<svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="3"/><path d="M10 10.5a2.2 2.2 0 1 0 0 3M17 10.5a2.2 2.2 0 1 0 0 3"/></svg>',
 audio:'<svg viewBox="0 0 24 24"><path d="M4 10v4h4l5 4V6L8 10zM16.5 8.5a5 5 0 0 1 0 7"/></svg>',
 speed:'<svg viewBox="0 0 24 24"><path d="M12 14l4-5M4.5 17a9 9 0 1 1 15 0"/></svg>',
 pip:'<svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"/><rect x="12" y="11" width="7" height="5" rx="1" fill="currentColor"/></svg>',
 air:'<svg viewBox="0 0 24 24"><path d="M5 17H4a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1h16a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1h-1M12 14l5 6H7z"/></svg>',
 fs:'<svg viewBox="0 0 24 24"><path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/></svg>',
 prev:'<svg viewBox="0 0 24 24"><path d="M6 6v12M20 6L9 12l11 6z"/></svg>',
 next:'<svg viewBox="0 0 24 24"><path d="M18 6v12M4 6l11 6-11 6z"/></svg>',
 back10:'<svg viewBox="0 0 24 24"><path d="M12 5V2L7 6l5 4V7a6 6 0 1 1-6 6" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
 fwd10:'<svg viewBox="0 0 24 24"><path d="M12 5V2l5 4-5 4V7a6 6 0 1 0 6 6" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
};

/* ================= الشبكة / API ================= */
const KINDS={
 movie:{cat:'get_vod_categories',list:'get_vod_streams',idk:'stream_id',img:'stream_icon'},
 series:{cat:'get_series_categories',list:'get_series',idk:'series_id',img:'cover'},
 live:{cat:'get_live_categories',list:'get_live_streams',idk:'stream_id',img:'stream_icon'},
};
const imgUrl=u=>{if(!u||typeof u!=='string')return'';u=u.trim();if(/^https:/i.test(u))return u;if(/^http:/i.test(u))return'/i?u='+encodeURIComponent(u);return''};
async function api(action,extra){
  let r;
  try{r=await fetch('/x'+(action?'?action='+action+(extra||''):''))}catch(e){const x=new Error('net');x.code='net';throw x}
  if(r.status===401){const x=new Error('auth');x.code='auth';throw x}
  if(r.status===409){showSetup('');const x=new Error('setup');x.code='setup';throw x}
  if(r.status===503){const x=new Error('cfg');x.code='cfg';throw x}
  if(r.status===502){const x=new Error('upstream');x.code='upstream';try{const j=await r.json();x.st=j.status;x.dg=[j.server,j.snip].filter(Boolean).join(' | ')}catch(e){}throw x}
  if(!r.ok){const x=new Error('http'+r.status);x.code='http';throw x}
  try{return await r.json()}catch(e){const x=new Error('json');x.code='json';throw x}
}
function errText(e){
  switch(e&&e.code){
    case'auth':return t('auth');case'cfg':return t('cfg');case'net':return t('net');
    case'upstream':return(e.st===403||e.st===401||e.st===451)?'السيرفر رفض الاتصال من خوادم الموقع (HTTP '+e.st+'). غالبًا المزوّد يحجب عناوين Cloudflare.':'تعذّر الاتصال بسيرفر الاشتراك'+(e.st?' (HTTP '+e.st+')':'')+'.';
    case'json':return'ردّ السيرفر غير مفهوم.';
    default:return'تعذّر الاتصال بالسيرفر.';
  }
}
/* كاش بسيط في IndexedDB لتسريع الفتح التالي (يُهمَل عند أي خطأ) */
const DB={
  _p:null,
  open(){if(this._p)return this._p;this._p=new Promise(res=>{try{const r=indexedDB.open('ayham_web',1);r.onupgradeneeded=()=>r.result.createObjectStore('c');r.onsuccess=()=>res(r.result);r.onerror=()=>res(null)}catch(e){res(null)}});return this._p},
  async get(k,ttl){const db=await this.open();if(!db)return null;return new Promise(res=>{try{const q=db.transaction('c').objectStore('c').get(k);q.onsuccess=()=>{const v=q.result;res(v&&now()-v.t<ttl?v.d:null)};q.onerror=()=>res(null)}catch(e){res(null)}})},
  async set(k,d){const db=await this.open();if(!db)return;try{db.transaction('c','readwrite').objectStore('c').put({t:now(),d},k)}catch(e){}},
  async clear(){const db=await this.open();if(!db)return;try{db.transaction('c','readwrite').objectStore('c').clear()}catch(e){}},
};
const TTL=25*60*1000;
async function cachedApi(action,extra,ttl){
  const key=action+(extra||'');
  const c=await DB.get(key,ttl||TTL);if(c)return c;
  const d=await api(action,extra);DB.set(key,d);return d;
}

/* ================= تطبيع البيانات والشارات ================= */
const TAGTOK='(?:8K|4K|UHD|FHD|HD|SD|1080P?|720P?|AR|EN|ARABIC|ENGLISH|MULTI|DUB|DUBBED|SUB|SUBBED|TR|FR|HINDI|KR|NF|AMZN|WEB|NETFLIX|VIP|HDR10?|DV|ATMOS)';
const PREFIX=new RegExp('^\\s*(?:[\\[(|]?'+TAGTOK+'(?:[+\\s]'+TAGTOK+')*[\\])|]?\\s*[-:|–—]+\\s*)+','i');
function parseName(n){
  let s=String(n||'').trim();const g={};
  const Q=/(?:^|[^A-Za-z0-9])(8K|4K|UHD|FHD|1080P?|HD|720P?)(?![A-Za-z0-9])/i.exec(s);
  if(Q){const q=Q[1].toUpperCase();g.q=q==='UHD'?'4K':(q==='FHD'||q.startsWith('1080'))?'FHD':q.startsWith('720')?'HD':q}
  g.hdr=/(?:^|[^A-Za-z0-9])(HDR10?\+?|DV|DOLBY ?VISION)(?![A-Za-z0-9])/i.test(s);
  g.ar=/(?:^|[^A-Za-z0-9])(AR|ARABIC)(?![A-Za-z0-9])|عربي/i.test(s);
  g.sub=/مترجم|(?:^|[^A-Za-z0-9])SUB(?:BED|S)?(?![A-Za-z0-9])/i.test(s);
  g.dub=/مدبلج|(?:^|[^A-Za-z0-9])DUB(?:BED)?(?![A-Za-z0-9])/i.test(s);
  s=s.replace(PREFIX,'');
  s=s.replace(/\s*[\[(]\s*(?:4K|FHD|HD|SD|UHD|AR|EN|MULTI|DUB|SUB|1080P?|720P?)[^\])]*[\])]\s*/gi,' ');
  let year='';const y=/[\[(]\s*((?:19|20)\d\d)\s*[\])]\s*$/.exec(s);
  if(y){year=y[1];s=s.slice(0,y.index)}
  s=s.replace(/\s{2,}/g,' ').trim();
  return{name:s||String(n||''),year,tags:g};
}
const AR_NORM=s=>String(s||'').toLowerCase().replace(/[ً-ٰٟـ]/g,'').replace(/[أإآٱ]/g,'ا').replace(/ى/g,'ي').replace(/ة/g,'ه').replace(/ؤ/g,'و').replace(/ئ/g,'ي');
class Item{
  constructor(k,id,full,img,rating,added,cat,ext,num){this.k=k;this.id=id;this.full=full;this.img=img;this.rating=rating;this.added=added;this.cat=cat;this.ext=ext;this.num=num}
  get _p(){return this.__p||(this.__p=parseName(this.full))}
  get name(){return this._p.name}
  get year(){return this._p.year}
  get tags(){return this._p.tags}
  get nn(){return this.__n!==undefined?this.__n:(this.__n=AR_NORM(this.full))}
}
function norm(k,raw){
  const c=KINDS[k];
  let r=parseFloat(raw.rating);if(!(r>0)){const f=parseFloat(raw.rating_5based);r=f>0?f*2:0}
  if(r>10)r=0;
  return new Item(k,String(raw[c.idk]),raw.name||raw.title||'',raw[c.img]||'',r,+(raw.added||raw.last_modified||0)||0,String(raw.category_id||''),raw.container_extension||'',+raw.num||0);
}
const ST={tab:'home',byAdded:{},cats:{},data:{},idx:{},loaded:{},catSel:{movie:'all',series:'all',live:null},sort:{movie:'new',series:'new'},info:{},hue:{},acct:null};
const DAY=864e5/1000;
const isNew=it=>it.added&&(now()/1000-it.added)<14*DAY;
async function ensure(k){
  if(ST.loaded[k])return;
  if(ST.loading&&ST.loading[k])return ST.loading[k];
  ST.loading=ST.loading||{};
  ST.loading[k]=(async()=>{
    const [cs,ls]=await Promise.all([cachedApi(KINDS[k].cat,'',60*60*1000),api(KINDS[k].list)]);
    ST.cats[k]=Array.isArray(cs)?cs:[];
    const arr=(Array.isArray(ls)?ls:[]).map(r=>norm(k,r));
    ST.data[k]=arr;ST.byAdded[k]=arr.slice().sort((x,y)=>y.added-x.added);const m=new Map();for(const it of arr)m.set(it.id,it);ST.idx[k]=m;ST.loaded[k]=true;
  })();
  try{await ST.loading[k]}finally{delete ST.loading[k]}
}

/* ================= قائمتي + التقدم ================= */
let _fav=null;const favs=()=>_fav||(_fav=store.get('fav2',[]));
const isFav=(k,id)=>favs().some(f=>f.k===k&&String(f.id)===String(id));
function toggleFav(it){
  const f=favs();const i=f.findIndex(x=>x.k===it.k&&String(x.id)===String(it.id));
  if(i>=0)f.splice(i,1);else f.unshift({k:it.k,id:it.id,name:it.name,img:it.img,ext:it.ext,at:now()});
  _fav=f.slice(0,400);store.set('fav2',_fav);return i<0;
}
let _pr=null;const getPr=()=>_pr||(_pr=store.get('pr2',{}));
function savePrEntry(e){
  const pr=getPr();pr[e.key]=e;
  const ks=Object.keys(pr);if(ks.length>120){ks.sort((a,b)=>pr[a].at-pr[b].at).slice(0,ks.length-120).forEach(k=>delete pr[k])}
  _pr=pr;store.set('pr2',pr);
}
const fracOf=key=>{const p=getPr()[key];return p&&p.d>0?Math.min(1,p.t/p.d):0};
function toast(m){const e=document.createElement('div');e.className='toast';e.textContent=m;document.body.appendChild(e);setTimeout(()=>e.remove(),2400)}

/* ================= لون من البوستر ================= */
function hueFromName(s){let h=0;for(const c of String(s))h=(h*31+c.charCodeAt(0))%360;return h}
function hsv2hex(h,s,v){const f=n=>{const k=(n+h/60)%6;const x=v-v*s*Math.max(0,Math.min(k,4-k,1));return Math.round(x*255)};return'#'+[f(5),f(3),f(1)].map(x=>x.toString(16).padStart(2,'0')).join('')}
function posterColor(url,fallbackName){
  const u=imgUrl(url);
  if(!u)return Promise.resolve(hsv2hex(hueFromName(fallbackName),.6,.85));
  if(ST.hue[u])return Promise.resolve(ST.hue[u]);
  return new Promise(res=>{
    const done=c=>{ST.hue[u]=c;res(c)};
    const fb=()=>done(hsv2hex(hueFromName(fallbackName),.6,.85));
    const im=new Image();im.crossOrigin='anonymous';
    const to=setTimeout(fb,3500);
    im.onload=()=>{clearTimeout(to);try{
      const w=24,h=36,cv=document.createElement('canvas');cv.width=w;cv.height=h;const cx=cv.getContext('2d',{willReadFrequently:true});cx.drawImage(im,0,0,w,h);
      const d=cx.getImageData(0,0,w,h).data;let sx=0,sy=0,tw=0,ss=0,n=0;
      for(let i=0;i<d.length;i+=4){
        const r=d[i]/255,g=d[i+1]/255,b=d[i+2]/255,mx=Math.max(r,g,b),mn=Math.min(r,g,b),df=mx-mn,s=mx?df/mx:0;
        let hh=0;if(df){hh=mx===r?((g-b)/df)%6:mx===g?(b-r)/df+2:(r-g)/df+4;hh*=60;if(hh<0)hh+=360}
        const wt=s*s*mx+.0005,rad=hh*Math.PI/180;sx+=Math.cos(rad)*wt;sy+=Math.sin(rad)*wt;tw+=wt;
        if(s>.25&&mx>.2){ss+=s;n++}
      }
      if(tw<1)return fb();
      const hue=(Math.atan2(sy,sx)*180/Math.PI+360)%360,sat=n?ss/n:.6;
      done(hsv2hex(hue,Math.max(.55,Math.min(.9,sat)),.88));
    }catch(e){fb()}};
    im.onerror=()=>{clearTimeout(to);fb()};
    im.src=u;
  });
}

/* ================= مكوّنات الواجهة ================= */
function badges(it,full){
  let h='';const g=it.tags||{};
  if(g.q==='4K'||g.q==='8K')h+='<span class="bdg b4k">'+g.q+'</span>';else if(g.q)h+='<span class="bdg bhd">'+g.q+'</span>';
  if(g.hdr)h+='<span class="bdg bhdr">HDR</span>';
  if(full){if(g.sub)h+='<span class="bdg bsub">'+(lang==='ar'?'مترجم':'SUB')+'</span>';if(g.dub)h+='<span class="bdg bar_">'+(lang==='ar'?'مدبلج':'DUB')+'</span>';else if(g.ar)h+='<span class="bdg bar_">AR</span>'}
  return h;
}
const rate=it=>it.rating>0?'<span class="bdg bstar">'+it.rating.toFixed(1)+'</span>':'';
function tile(it,o){
  o=o||{};const img=imgUrl(it.img);const key=(it.k==='movie'?'movie:':it.k==='series'?'series:':'live:')+it.id;
  const fr=o.pg!=null?o.pg:(it.k==='movie'?fracOf('movie:'+it.id):0);
  const pg=fr>.01&&fr<.97?'<div class="pg"><i style="width:'+Math.round(fr*100)+'%"></i></div>':'';
  const ph=img?'':'<div class="ph">'+esc(it.name)+'</div>';
  const im=img?'<img src="'+esc(img)+'" loading="lazy" decoding="async" alt="" onload="this.classList.add(\'ld\')" onerror="this.remove()">':'';
  const inner='<div class="im">'+ph+im+'<div class="tl">'+badges(it)+'</div>'+(isNew(it)?'<div class="tn"><span class="bdg bnew">'+(lang==='ar'?'جديد':'NEW')+'</span></div>':'')+'<div class="tr">'+rate(it)+'</div>'+pg+'</div>';
  if(o.rank)return'<button class="tile rk press" data-a="open" data-i="'+it.k+':'+esc(it.id)+'"><span class="num">'+o.rank+'</span><div class="col">'+inner+'<span class="tt">'+esc(it.name)+'</span></div></button>';
  return'<button class="tile press" data-a="open" data-i="'+it.k+':'+esc(it.id)+'">'+inner+'<span class="tt">'+esc(it.name)+'</span></button>';
}
function contTile(p){
  const it=p.item||{};const img=imgUrl(it.img);const fr=p.d?Math.min(1,p.t/p.d):0;
  const left=Math.max(1,Math.round((p.d-p.t)/60));
  return'<button class="tile land press" data-a="cont" data-pk="'+esc(p.key)+'"><div class="im">'+(img?'<img class="bgi" src="'+esc(img)+'" alt="" onload="this.classList.add(\'ld\')" onerror="this.remove()">':'')+
   '<div class="mini">'+(img?'<img src="'+esc(img)+'" alt="">':'')+'</div><div class="ctx"><b>'+esc(p.title||it.name||'')+'</b><small>'+left+' '+t('min')+(lang==='ar'?' متبقية':' left')+'</small></div>'+
   '<div class="play"><span>'+I.play+'</span></div><div class="pg"><i style="width:'+Math.round(fr*100)+'%"></i></div></div></button>';
}
function rail(title,items,o){
  o=o||{};if(!items||!items.length)return'';
  const rk=o.rank;
  return'<div class="rowt"><span>'+esc(title)+'</span>'+(o.more?'<small data-a="'+o.more+'">'+t('more')+' ‹</small>':'')+'</div><div class="rail">'+
    items.map((it,i)=>o.html?o.html(it,i):tile(it,{rank:rk?i+1:0})).join('')+'</div>';
}
const skelRail='<div class="rail">'+'<div class="skl" style="flex:none;width:118px;aspect-ratio:2/3"></div>'.repeat(6)+'</div>';

/* ================= الدخول ================= */
let authed=false;
function devId(){try{let d=localStorage.ayd;if(!d){d=Array.from(crypto.getRandomValues(new Uint8Array(9)),x=>(x%36).toString(36)).join('');localStorage.ayd=d}return d}catch(e){return'x'}}
function showLogin(msg){
  $('#app').classList.add('hide');$('#nav').classList.add('hide');$('#top').classList.add('hide');$('#login').classList.remove('hide');
  $('#lmsg').textContent=msg||'';authed=false;
}
$('#lf').addEventListener('submit',async ev=>{
  ev.preventDefault();
  const pw=$('#in-pwd').value;const m=$('#lmsg'),b=$('#lbtn');
  if(!pw){m.textContent=t('enter');return}
  b.disabled=true;b.textContent='…';m.textContent='';
  let r;try{r=await fetch('/login',{method:'POST',body:JSON.stringify({password:pw,device:devId()})})}catch(e){m.textContent=t('net');b.disabled=false;b.textContent=t('login');return}
  b.disabled=false;b.textContent=t('login');
  if(r.status===503){m.textContent=t('cfg');return}
  if(!r.ok){let e='';try{e=(await r.json()).err}catch(x){}
    m.textContent=lang==='ar'?'كلمة المرور غير صحيحة.':'Wrong password.';return}
  await afterAuth();
});
async function afterAuth(){
  try{const j=await (await fetch('/me')).json();if(j.setup)return showSetup('');}catch(e){}
  startApp();
}
let setupBack=false;
function showSetup(msg){
  $('#app').classList.add('hide');$('#nav').classList.add('hide');$('#top').classList.add('hide');$('#login').classList.add('hide');
  $('#setup').classList.remove('hide');$('#smsg').textContent=msg||'';
  $('#scancel').classList.toggle('hide',!setupBack);authed=false;
}
$('#sf').addEventListener('submit',async ev=>{
  ev.preventDefault();
  const m=$('#smsg'),b=$('#sbtn');
  const url=$('#s-url').value.trim(),user=$('#s-user').value.trim(),pass=$('#s-pass').value;
  if(!/^https?:\/\//i.test(url)){m.textContent='الرابط يجب أن يبدأ بـ http:// أو https://';return}
  if(!user||!pass){m.textContent='اكتب اسم المستخدم وكلمة المرور.';return}
  b.disabled=true;b.textContent='…';m.textContent='';
  let r,j={};
  try{r=await fetch('/setup',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({url,user,pass})});try{j=await r.json()}catch(e){}}
  catch(e){m.textContent=t('net');b.disabled=false;b.textContent=t('login');return}
  b.disabled=false;b.textContent=lang==='ar'?'حفظ والدخول':'Save & sign in';
  if(!r.ok){
    m.textContent=j.err==='wrong_creds'?'اسم المستخدم أو كلمة المرور غير صحيحة لهذا الاشتراك.'
      :j.err==='bad_url'?'هذا الرابط غير مسموح (عنوان محلي أو داخلي).'
      :j.err==='upstream'?'تعذّر الوصول للسيرفر من الخادم. تأكد من الرابط والمنفذ (مثل :8080).'
      :'تحقق من البيانات وحاول مرة أخرى.';
    return;
  }
  setupBack=false;$('#setup').classList.add('hide');
  $('#sf').reset();
  startApp();
});
$('#scancel').onclick=()=>{$('#setup').classList.add('hide');startApp()};
async function logout(){try{await fetch('/logout',{method:'POST'})}catch(e){}ST.cats={};ST.data={};ST.idx={};ST.loaded={};await DB.clear();closeAllLayers();showLogin('')}

/* ================= الهيكل + التنقل ================= */
const TABS=[['home','home'],['movie','movies'],['series','series'],['live','live'],['fav','mylist']];
const TABICON={home:I.home,movie:I.movie,series:I.series,live:I.live,fav:I.list};
function renderNav(){
  $('#nav').innerHTML=TABS.map(([k,l])=>'<button data-a="tab" data-t="'+k+'" class="'+(ST.tab===k?'on':'')+'">'+TABICON[k]+'<span>'+t(l)+'</span></button>').join('');
}
function renderTop(){
  $('#top').innerHTML='<img src="/icon-192.png" alt=""><b>Ayham Player</b><span class="sp"></span><button class="ib press" data-a="search" aria-label="'+t('search')+'">'+I.search+'</button><button class="ib press" data-a="menu" aria-label="'+t('menu')+'">'+I.menu+'</button>';
  $('#top').classList.toggle('solid',ST.tab!=='home'||window.scrollY>40);
}
function startApp(){
  authed=true;
  $('#login').classList.add('hide');$('#app').classList.remove('hide');$('#nav').classList.remove('hide');$('#top').classList.remove('hide');
  renderTop();renderNav();go(ST.tab||'home');
  if(/iphone|ipad|ipod/i.test(navigator.userAgent)&&!navigator.standalone&&!store.get('a2h',0)){
    setTimeout(()=>{if(!authed)return;const d=document.createElement('div');d.className='a2h';d.innerHTML='<button data-a="a2hx">×</button>'+t('a2h');document.body.appendChild(d)},2500);
  }
  preload();
}
async function preload(){try{await Promise.all([ensure('movie'),ensure('series')])}catch(e){}}
function go(tab){
  ST.tab=tab;renderNav();renderTop();window.scrollTo(0,0);stopHero();
  const v=$('#view');v.style.animation='none';v.offsetHeight;v.style.animation='';
  if(tab==='home')return viewHome();
  if(tab==='fav')return viewFav();
  if(tab==='live')return viewLive();
  return viewKind(tab);
}
window.addEventListener('scroll',()=>{const tp=$('#top');if(tp)tp.classList.toggle('solid',ST.tab!=='home'||window.scrollY>40)},{passive:true});

function retryBox(e,fn){
  const v=$('#view');v.innerHTML='<div class="empty">'+esc(errText(e))+'<br><br><button class="bs" style="margin:auto" id="rt">'+t('retry')+'</button></div>';
  $('#rt').onclick=fn;
}

/* ================= الرئيسية ================= */
let heroTimer=0;
function stopHero(){clearInterval(heroTimer);heroTimer=0}
function recentPool(arr,n){const c=arr===ST.data.movie?ST.byAdded.movie:arr===ST.data.series?ST.byAdded.series:null;return(c||arr.slice().sort((a,b)=>b.added-a.added)).slice(0,n)}
function topRated(arr,n,pool){
  const p=recentPool(arr,pool||700).filter(x=>x.rating>=5&&x.img);
  return p.sort((a,b)=>b.rating-a.rating||b.added-a.added).slice(0,n);
}
async function viewHome(){
  const v=$('#view');
  v.innerHTML='<div class="hero skl" style="border-radius:0"></div>'+skelRail+skelRail;
  try{await Promise.all([ensure('movie'),ensure('series')])}catch(e){if(e.code==='auth')return showLogin(errText(e));return retryBox(e,viewHome)}
  if(ST.tab!=='home')return;
  const M=ST.data.movie,S=ST.data.series;
  const top10m=topRated(M,10),top10s=topRated(S,10);
  const heroPool=recentPool(M,300).concat(recentPool(S,300)).filter(x=>x.rating>=6&&x.img).sort((p,q)=>q.added-p.added).slice(0,60);
  const heroItems=[];const seen=new Set();
  // مزيج: أعلى تقييمًا من الأحدث، متنوّع بين أفلام ومسلسلات
  for(const x of heroPool.sort((a,b)=>b.rating-a.rating)){if(seen.has(x.name))continue;seen.add(x.name);heroItems.push(x);if(heroItems.length>=6)break}
  const pr=getPr();
  const cont=Object.values(pr).filter(p=>p.d>0&&p.t>10&&p.t<p.d*0.93).sort((a,b)=>b.at-a.at).slice(0,15);
  const mine=favs().map(f=>(ST.idx[f.k]&&ST.idx[f.k].get(String(f.id)))||{k:f.k,id:String(f.id),name:f.name,img:f.img,ext:f.ext,tags:{},rating:0,added:0}).slice(0,25);
  let h=heroHtml(heroItems,top10m.concat(top10s));
  if(cont.length)h+=rail(t('cont'),cont,{html:p=>contTile(p)});
  h+=rail(t('top10m'),top10m,{rank:true});
  h+=rail(t('newm'),recentPool(M,24),{more:'goto:movie'});
  h+=rail(t('top10s'),top10s,{rank:true});
  h+=rail(t('news'),recentPool(S,24),{more:'goto:series'});
  if(mine.length)h+=rail(t('mylist'),mine,{more:'goto:fav'});
  // صفوف أنواع (تصنيفات كبيرة)
  const catRows=(k,arr,cs)=>{
    const cnt={};for(const x of arr)cnt[x.cat]=(cnt[x.cat]||0)+1;
    return cs.filter(c=>cnt[c.category_id]>=12&&!/^\s*(all|الكل)\s*$/i.test(c.category_name)).sort((a,b)=>cnt[b.category_id]-cnt[a.category_id]).slice(0,3).map(c=>{
      const items=arr.filter(x=>x.cat===String(c.category_id)).sort((a,b)=>b.added-a.added).slice(0,18);
      return rail(cleanCat(c.category_name),items);
    }).join('');
  };
  h+=catRows('movie',M,ST.cats.movie)+catRows('series',S,ST.cats.series);
  v.innerHTML=h;
  setupHero(heroItems);
}
function cleanCat(n){return String(n||'').replace(/^\s*(?:[A-Z0-9+]{1,5}\s*[|:\-–]+\s*)+/i,'').trim()||String(n||'')}
function rankOf(it,tops){const i=tops.findIndex(x=>x.k===it.k&&x.id===it.id);return i>=0&&i<10?i+1:0}
function heroHtml(items,tops){
  if(!items.length)return'<div style="height:calc(72px + env(safe-area-inset-top))"></div>';
  const slides=items.map((it,i)=>{
    const rk=rankOf(it,tops);const img=imgUrl(it.img);
    return'<div class="hslide" data-a="open" data-i="'+it.k+':'+esc(it.id)+'" data-n="'+i+'"><img class="bg" src="'+esc(img)+'" alt="" '+(i?'loading="lazy"':'fetchpriority="high"')+'>'+
      '<div class="hinfo"><div class="hmeta">'+(rk?'<span class="bdg brank">'+(lang==='ar'?'🔥 رقم '+rk+' اليوم':'🔥 #'+rk+' today')+'</span>':'')+badges(it,true)+(it.rating?'<span class="bdg bimdb">'+it.rating.toFixed(1)+'</span>':'')+(it.year?'<span>'+esc(it.year)+'</span>':'')+'<span class="bdg bgenre">'+t(it.k==='series'?'series':'movies')+'</span></div>'+
      '<h2>'+esc(it.name)+'</h2><div class="hbtns"><button class="bp press" data-a="hplay" data-i="'+it.k+':'+esc(it.id)+'">'+I.play+t('play')+'</button><button class="bs press" data-a="hfav" data-i="'+it.k+':'+esc(it.id)+'">'+(isFav(it.k,it.id)?I.check:I.plus)+'<span>'+t('mylist')+'</span></button></div></div></div>';
  }).join('');
  return'<div class="hero"><div class="hs" id="hs">'+slides+'</div><div class="dots" id="hd">'+items.map((_,i)=>'<i class="'+(i?'':'on')+'"></i>').join('')+'</div></div>';
}
function setupHero(items){
  const hs=$('#hs');if(!hs)return;
  const dots=$$('#hd i');let idx=0,touch=0;
  const setHue=async i=>{const it=items[i];if(!it)return;const c=await posterColor(it.img,it.name);const hero=$('.hero');if(hero&&ST.tab==='home')hero.style.setProperty('--hc',c+'66')};
  setHue(0);
  hs.addEventListener('scroll',()=>{const w=hs.clientWidth||1;const i=Math.round(Math.abs(hs.scrollLeft)/w);if(i!==idx){idx=i;dots.forEach((d,j)=>d.classList.toggle('on',j===i));setHue(i)}},{passive:true});
  hs.addEventListener('touchstart',()=>{touch=now()},{passive:true});
  stopHero();
  heroTimer=setInterval(()=>{
    if(ST.tab!=='home'||document.hidden||now()-touch<9000||!$('#hs')||layers.length)return;
    const n=(idx+1)%items.length;hs.scrollTo({left:n*hs.clientWidth*(getComputedStyle(hs).direction==='rtl'?-1:1),behavior:'smooth'});
  },6500);
}

/* ================= الأفلام / المسلسلات ================= */
const PAGE=48;
function sortItems(arr,mode){
  const a=arr.slice();
  if(mode==='rated')return a.sort((x,y)=>y.rating-x.rating||y.added-x.added);
  if(mode==='az')return a.sort((x,y)=>x.name.localeCompare(y.name,lang));
  return a.sort((x,y)=>y.added-x.added||y.num-x.num);
}
async function viewKind(k){
  const v=$('#view');
  v.innerHTML='<div class="tabhead"><h1>'+t(k==='movie'?'movies':'series')+'</h1></div>'+skelRail+skelRail;
  try{await ensure(k)}catch(e){if(e.code==='auth')return showLogin(errText(e));return retryBox(e,()=>viewKind(k))}
  if(ST.tab!==k)return;
  const cs=ST.cats[k],sel=ST.catSel[k];
  let h='<div class="tabhead"><h1>'+t(k==='movie'?'movies':'series')+'</h1>'+(sel!=='all'?'<button class="sort press" data-a="sort" data-k="'+k+'">'+t({new:'newest',rated:'rated',az:'az'}[ST.sort[k]])+' ▾</button>':'')+'</div>';
  h+='<div class="chips" id="chips"><button class="chip '+(sel==='all'?'on':'')+'" data-a="cat" data-k="'+k+'" data-c="all">'+t('all')+'</button>'+cs.map(c=>'<button class="chip '+(String(c.category_id)===sel?'on':'')+'" data-a="cat" data-k="'+k+'" data-c="'+esc(c.category_id)+'">'+esc(cleanCat(c.category_name))+'</button>').join('')+'</div><div id="kb"></div>';
  v.innerHTML=h;
  const on=$('#chips .on');if(on&&on.scrollIntoView)on.scrollIntoView({inline:'center',block:'nearest'});
  const kb=$('#kb');
  if(sel==='all'){
    const arr=ST.data[k];
    let rows=rail(t(k==='movie'?'newm':'news'),recentPool(arr,24))+rail(t(k==='movie'?'top10m':'top10s'),topRated(arr,10),{rank:true});
    const cnt={};for(const x of arr)cnt[x.cat]=(cnt[x.cat]||0)+1;
    const big=cs.filter(c=>cnt[c.category_id]>=6).sort((a,b)=>cnt[b.category_id]-cnt[a.category_id]).slice(0,14);
    rows+=big.map(c=>{const items=arr.filter(x=>x.cat===String(c.category_id)).sort((a,b)=>b.added-a.added).slice(0,20);
      return'<div class="rowt"><span>'+esc(cleanCat(c.category_name))+'</span><small data-a="cat" data-k="'+k+'" data-c="'+esc(c.category_id)+'">'+t('more')+' ‹</small></div><div class="rail">'+items.map(it=>tile(it)).join('')+'</div>'}).join('');
    kb.innerHTML=rows;
  }else drawGrid(k,sel);
}
function drawGrid(k,cat){
  const kb=$('#kb');const items=sortItems(ST.data[k].filter(x=>x.cat===String(cat)),ST.sort[k]);
  if(!items.length){kb.innerHTML='<div class="empty">'+t('empty')+'</div>';return}
  let shown=0;
  kb.innerHTML='<div class="grid" id="gd"></div><div id="sent" style="height:40px"></div>';
  const gd=$('#gd');
  const more=()=>{const next=items.slice(shown,shown+PAGE);shown+=next.length;gd.insertAdjacentHTML('beforeend',next.map(it=>tile(it)).join(''));if(shown>=items.length&&io)io.disconnect()};
  let io=null;
  if('IntersectionObserver' in window){io=new IntersectionObserver(es=>{if(es.some(e=>e.isIntersecting))more()},{rootMargin:'900px'});io.observe($('#sent'))}
  more();if(!io)while(shown<items.length)more();
}

/* ================= البث المباشر ================= */
const epgCache=new Map();
function b64u(s){try{return decodeURIComponent(escape(atob(s)))}catch(e){try{return atob(s)}catch(x){return''}}}
async function epgFor(id){
  const c=epgCache.get(id);if(c&&now()-c.t<180000)return c.d;
  let d=[];
  try{const j=await api('get_short_epg','&stream_id='+encodeURIComponent(id)+'&limit=2');
    d=(j.epg_listings||[]).map(e=>({title:b64u(e.title||''),s:+e.start_timestamp||0,e:+e.stop_timestamp||0})).filter(e=>e.title)}catch(e){}
  epgCache.set(id,{t:now(),d});return d;
}
const epgQ=[];let epgBusy=0;
function pumpEpg(){
  while(epgBusy<3&&epgQ.length){
    const fn=epgQ.shift();epgBusy++;fn().finally(()=>{epgBusy--;pumpEpg()});
  }
}
function paintEpg(el,d){
  if(!el||!d.length)return;const n=Math.floor(now()/1000);
  const cur=d.find(x=>x.s<=n&&x.e>n)||d[0];const small=$('small',el),bar=$('.eb i',el);
  if(small)small.textContent=cur.title;
  if(bar&&cur.e>cur.s)bar.style.width=Math.max(2,Math.min(100,(n-cur.s)/(cur.e-cur.s)*100))+'%';
}
function liveRow(it){
  const img=imgUrl(it.img);
  return'<button class="lrow press" data-a="open" data-i="live:'+esc(it.id)+'" data-eid="'+esc(it.id)+'"><div class="lg">'+(img?'<img src="'+esc(img)+'" loading="lazy" alt="" onerror="this.remove()">':'<b>'+esc((it.name||'?').trim().charAt(0))+'</b>')+'</div><div class="li"><b>'+esc(it.name)+'</b><small></small><div class="eb"><i style="width:0"></i></div></div>'+badges(it)+'<span class="lv"></span></button>';
}
async function viewLive(){
  const v=$('#view');
  v.innerHTML='<div class="tabhead"><h1>'+t('live')+'</h1></div><div class="empty">'+t('loading')+'</div>';
  try{await ensure('live')}catch(e){if(e.code==='auth')return showLogin(errText(e));return retryBox(e,viewLive)}
  if(ST.tab!=='live')return;
  const cs=ST.cats.live;let sel=ST.catSel.live;
  if(!sel||!cs.find(c=>String(c.category_id)===sel))sel=cs.length?String(cs[0].category_id):'';
  ST.catSel.live=sel;
  v.innerHTML='<div class="tabhead"><h1>'+t('live')+'</h1></div><div class="chips" id="chips">'+cs.map(c=>'<button class="chip '+(String(c.category_id)===sel?'on':'')+'" data-a="cat" data-k="live" data-c="'+esc(c.category_id)+'">'+esc(cleanCat(c.category_name))+'</button>').join('')+'</div><div id="kb"></div>';
  const on=$('#chips .on');if(on&&on.scrollIntoView)on.scrollIntoView({inline:'center',block:'nearest'});
  const items=ST.data.live.filter(x=>x.cat===sel).sort((a,b)=>a.num-b.num);
  const kb=$('#kb');if(!items.length){kb.innerHTML='<div class="empty">'+t('empty')+'</div>';return}
  ST.liveList=items;let shown=0;kb.innerHTML='<div id="ll" style="padding-top:6px"></div><div id="sent" style="height:40px"></div>';
  const ll=$('#ll');
  const eio='IntersectionObserver' in window?new IntersectionObserver(es=>es.forEach(e=>{if(!e.isIntersecting)return;eio.unobserve(e.target);const el=e.target,id=el.dataset.eid;epgQ.push(async()=>{paintEpg(el,await epgFor(id))});pumpEpg()}),{rootMargin:'200px'}):null;
  const more=()=>{const next=items.slice(shown,shown+40);shown+=next.length;const start=ll.children.length;ll.insertAdjacentHTML('beforeend',next.map(liveRow).join(''));if(eio)for(let i=start;i<ll.children.length;i++)eio.observe(ll.children[i])};
  let io=null;
  if('IntersectionObserver' in window){io=new IntersectionObserver(es=>{if(es.some(e=>e.isIntersecting)&&shown<items.length)more()},{rootMargin:'700px'});io.observe($('#sent'))}
  more();
}

/* ================= قائمتي ================= */
function viewFav(){
  const v=$('#view');const pr=getPr();
  const cont=Object.values(pr).filter(p=>p.d>0&&p.t>10&&p.t<p.d*0.93).sort((a,b)=>b.at-a.at).slice(0,30);
  const f=favs().map(x=>(ST.idx[x.k]&&ST.idx[x.k].get(String(x.id)))||{k:x.k,id:String(x.id),name:x.name,img:x.img,ext:x.ext,tags:{},rating:0,added:0});
  let h='<div class="tabhead"><h1>'+t('mylist')+'</h1></div>';
  if(cont.length)h+=rail(t('cont'),cont,{html:p=>contTile(p)});
  h+=f.length?'<div class="rowt"><span>'+t('mylist')+'</span></div><div class="grid">'+f.map(it=>it.k==='live'?'':tile(it)).join('')+'</div>'+
    (f.some(x=>x.k==='live')?'<div class="rowt"><span>'+t('live')+'</span></div><div style="padding-top:8px">'+f.filter(x=>x.k==='live').map(liveRow).join('')+'</div>':''):
    '<div class="empty">'+(lang==='ar'?'قائمتك فارغة.<br>اضغط ＋ على أي فيلم أو مسلسل لإضافته هنا.':'Your list is empty.<br>Tap ＋ on any title to add it here.')+'</div>';
  v.innerHTML=h;
}

/* ================= البحث ================= */
let sTimer=0;
function openSearch(){
  if($('#srch'))return;
  const d=document.createElement('div');d.id='srch';
  d.innerHTML='<div class="sbar"><div class="f">'+I.search+'<input id="q" type="search" enterkeyhint="search" placeholder="'+esc(t('srch_ph'))+'" autocomplete="off"></div><button class="ib press" data-a="srchx">'+I.close+'</button></div><div id="sr"><div class="empty">'+t('srch_min')+'</div></div>';
  document.body.appendChild(d);document.body.style.overflow='hidden';
  const q=$('#q');q.focus();q.oninput=()=>{clearTimeout(sTimer);sTimer=setTimeout(()=>runSearch(q.value),280)};
  pushLayer(()=>{d.remove();document.body.style.overflow=layers.length?'hidden':''});
}
async function runSearch(raw){
  const out=$('#sr');if(!out)return;
  const toks=AR_NORM(raw).split(/\s+/).filter(Boolean);
  if(AR_NORM(raw).replace(/\s/g,'').length<2){out.innerHTML='<div class="empty">'+t('srch_min')+'</div>';return}
  out.innerHTML='<div class="empty">'+t('searching')+'</div>';
  try{
    const same=()=>$('#q')&&AR_NORM($('#q').value)===AR_NORM(raw);
    const paint=()=>{
      let h='';
      for(const k of['movie','series','live']){
        if(!ST.loaded[k])continue;
        const res=ST.data[k].filter(it=>toks.every(x=>it.nn.includes(x)));
        res.sort((a,b)=>(b.nn.startsWith(toks[0])?1:0)-(a.nn.startsWith(toks[0])?1:0)||b.added-a.added);
        const top=res.slice(0,40);
        if(top.length)h+='<div class="rowt"><span>'+t(k==='movie'?'movies':k==='series'?'series':'live')+' ('+res.length+')</span></div>'+(k==='live'?'<div style="padding-top:8px">'+top.map(liveRow).join('')+'</div>':'<div class="grid">'+top.map(it=>tile(it)).join('')+'</div>');
      }
      out.innerHTML=h||'<div class="empty">'+(ST.loaded.live?t('srch_none'):t('searching'))+'</div>';
    };
    await Promise.all([ensure('movie'),ensure('series')]);
    if(!same())return;
    paint();                                   // أفلام ومسلسلات أولًا (سريع)
    if(!ST.loaded.live){await ensure('live').catch(()=>{});if(!same())return;paint()}
  }catch(e){out.innerHTML='<div class="empty">'+esc(errText(e))+'</div>'}
}

/* ================= طبقات + زر الرجوع (سحب الحافة في iOS) ================= */
const layers=[];
function pushLayer(close){layers.push(close);try{history.pushState({l:layers.length},'')}catch(e){}}
function popLayerUI(){if(layers.length)history.back()}
window.addEventListener('popstate',()=>{
  // عدد الطبقات أكبر من حالة السجل => أغلق الأعلى
  const want=(history.state&&history.state.l)||0;
  while(layers.length>want){const c=layers.pop();try{c()}catch(e){}}
});
function closeAllLayers(){while(layers.length){const c=layers.pop();try{c()}catch(e){}}}

/* ================= صفحة التفاصيل ================= */
async function getInfo(it){
  const key=it.k+':'+it.id;if(ST.info[key])return ST.info[key];
  const j=await cachedApi(it.k==='movie'?'get_vod_info':'get_series_info',(it.k==='movie'?'&vod_id=':'&series_id=')+encodeURIComponent(it.id),12*60*60*1000);
  ST.info[key]=j;return j;
}
function minsOf(d){if(!d)return'';const m=/^(\d+):(\d+)/.exec(d);if(m)return(+m[1]?+m[1]+(lang==='ar'?'س ':'h '):'')+(+m[2]?+m[2]+(lang==='ar'?'د':'m'):'');return/^\d+$/.test(d)?d+(lang==='ar'?' د':'m'):String(d)}
function similarTo(it){
  const tok=String(it.nn||AR_NORM(it.name)).split(/\s+/).filter(x=>x.length>2).slice(0,3);
  const pool=ST.data[it.k]||[];
  const sc=pool.filter(x=>x.id!==it.id&&x.cat===it.cat&&x.img).map(x=>({x,s:tok.reduce((a,w)=>a+(x.nn.includes(w)?2:0),0)+x.rating*0.1+(x.added/1e10)}));
  return sc.sort((a,b)=>b.s-a.s).slice(0,18).map(o=>o.x);
}
function openDetail(it){
  if(it.k==='live')return playLive(it,ST.liveList||[it]);
  const sh=$('#sheet');sh.classList.remove('hide');document.body.style.overflow='hidden';sh.scrollTop=0;
  sh.style.setProperty('--ac','#8b5cf6');
  const img=imgUrl(it.img),key=it.k+':'+it.id;
  const isS=it.k==='series';
  sh.innerHTML='<button class="ib back press" data-a="shx">'+I.back+'</button><div class="dh"><img class="bgi" id="dbg" src="'+esc(img)+'" alt=""><div class="dp">'+(img?'<img src="'+esc(img)+'" alt="">':'')+'</div></div>'+
   '<div class="dd"><h2>'+esc(it.name)+'</h2><div class="meta" id="dm">'+badges(it,true)+(it.rating?'<span class="bdg bimdb">'+it.rating.toFixed(1)+'</span>':'')+(it.year?'<span class="bdg bgenre">'+esc(it.year)+'</span>':'')+'</div>'+
   '<div class="acts"><button class="bp press" id="dplay">'+I.play+'<span id="dplt">'+t('play')+'</span></button><button class="ci press" id="dfav" aria-label="fav"></button></div>'+
   '<div class="plot" id="dpl"></div><span class="rm hide" id="drm" data-a="plot">'+t('read')+'</span>'+
   '<div class="sect hide" id="dsea"></div><div class="sect hide" id="dcast"></div><div class="sect hide" id="dsim"></div></div>';
  const setFav=()=>{const b=$('#dfav');if(!b)return;const on=isFav(it.k,it.id);b.classList.toggle('on',on);b.innerHTML=on?I.check:I.plus};setFav();
  $('#dfav').onclick=()=>{const on=toggleFav(it);setFav();toast(on?t('inl'):t('addl'))};
  posterColor(it.img,it.name).then(c=>{sh.style.setProperty('--ac',c)});
  pushLayer(()=>{sh.classList.add('hide');sh.innerHTML='';document.body.style.overflow=layers.length?'hidden':''});
  const sims=similarTo(it);
  const simHtml=()=>{if(sims.length>=3){const d=$('#dsim');d.classList.remove('hide');d.innerHTML='<h3>'+t('similar')+'</h3><div class="sheet-rail"><div class="rail">'+sims.map(x=>tile(x)).join('')+'</div></div>'}};
  const myOpen=sh;
  (async()=>{
    let j;try{j=await getInfo(it)}catch(e){if(e.code==='auth')return showLogin(errText(e));$('#dpl').textContent=errText(e);simHtml();setupPlay(it,null,null);return}
    if(sh.classList.contains('hide'))return;
    const i=j.info||{};
    const bd=(Array.isArray(i.backdrop_path)?i.backdrop_path[0]:i.backdrop_path)||'';
    if(bd){const bu=imgUrl(bd);if(bu){const b=$('#dbg');if(b)b.src=bu}}
    const genres=String(i.genre||'').split(/[,،\/]/).map(s=>s.trim()).filter(Boolean).slice(0,3);
    const rel=String(i.releasedate||i.releaseDate||i.release_date||'').slice(0,4);
    const dur=isS?'':minsOf(i.duration);
    const r=parseFloat(i.rating)||it.rating;
    $('#dm').innerHTML=badges(it,true)+(r?'<span class="bdg bimdb">'+(+r).toFixed(1)+'</span>':'')+(rel||it.year?'<span class="bdg bgenre">'+esc(rel||it.year)+'</span>':'')+genres.map(g=>'<span class="bdg bgenre">'+esc(g)+'</span>').join('')+(dur?'<span class="bdg bgenre">'+esc(dur)+'</span>':'');
    const plot=String(i.plot||i.description||'').trim();
    const pl=$('#dpl');pl.textContent=plot;
    if(plot.length>150){const rm=$('#drm');rm.classList.remove('hide')}
    const cast=String(i.cast||i.actors||'').split(/[,،]/).map(s=>s.trim()).filter(s=>s.length>2).slice(0,10);
    if(cast.length){const c=$('#dcast');c.classList.remove('hide');c.innerHTML='<h3>'+t('cast')+'</h3><div class="cast">'+cast.map(n=>'<span>'+esc(n)+'</span>').join('')+'</div>'}
    simHtml();
    if(isS)seriesUi(it,j);else{setupPlay(it,j,null)}
  })();
}
function setupPlay(it,j,ep){
  const b=$('#dplay');if(!b)return;
  if(it.k==='movie'){
    const p=getPr()['movie:'+it.id];const res=p&&p.t>10&&p.d&&p.t<p.d*.93;
    if(res)$('#dplt').textContent=t('resume')+' · '+Math.round((p.d-p.t)/60)+t('min');
    b.onclick=()=>{const ext=((j&&j.movie_data&&j.movie_data.container_extension)||it.ext||'mp4').replace(/[^a-z0-9]/gi,'')||'mp4';
      startPlayer({title:it.name,key:'movie:'+it.id,live:false,urls:srcList('movie',it.id,ext),item:{k:'movie',id:it.id,name:it.name,img:it.img,ext},resumeAt:res?p.t:0,poster:it.img})};
  }
}
function srcList(kind,id,ext){
  ext=(ext||'mp4').toLowerCase();const out=[ext];for(const x of['mp4','m3u8'])if(!out.includes(x))out.push(x);
  return out.map(e=>'/s/'+kind+'/'+encodeURIComponent(id)+'.'+e);
}
function seasonsOf(j){
  const eps=j.episodes||{};const arr=Array.isArray(eps)?eps.map((e,n)=>[String(n+1),e]):Object.entries(eps);
  return arr.map(([s,l])=>[s,(l||[]).map((e,n)=>({id:String(e.id),num:+e.episode_num||n+1,title:e.title||'',ext:e.container_extension||'mp4',plot:(e.info&&e.info.plot)||'',dur:(e.info&&(e.info.duration||''))||'',img:(e.info&&e.info.movie_image)||''}))]).sort((a,b)=>(+a[0])-(+b[0]));
}
function seriesUi(it,j){
  const seasons=seasonsOf(j);const box=$('#dsea');
  if(!seasons.length){box.classList.remove('hide');box.innerHTML='<div class="empty" style="padding:20px">'+t('empty')+'</div>';return}
  box.classList.remove('hide');
  const saved=store.get('lastsea:'+it.id,null);
  // ابدأ بالموسم الذي فيه الحلقة التالية
  let cur=seasons[0][0];
  const lastKey=store.get('lastep:'+it.id,null);
  if(lastKey){const f=seasons.find(([s,l])=>l.some(e=>'ep:'+e.id===lastKey));if(f)cur=f[0]}else if(saved&&seasons.find(s=>s[0]===saved))cur=saved;
  const nextIdx=list=>{
    const pe=list.findIndex(e=>{const f=fracOf('ep:'+e.id);return f>.01&&f<.93});if(pe>=0)return pe;
    const un=list.findIndex(e=>fracOf('ep:'+e.id)<.93);return un>=0?un:0;
  };
  const playEp=(s,idx)=>{
    const list=(seasons.find(x=>x[0]===s)||[0,[]])[1];
    startEpisodeQueue(it,j,seasons,s,idx);
  };
  const draw=()=>{
    const list=(seasons.find(x=>x[0]===cur)||[0,[]])[1];
    const done=list.filter(e=>fracOf('ep:'+e.id)>=.93).length;const ni=nextIdx(list);
    box.innerHTML=(seasons.length>1?'<div class="ss">'+seasons.map(([s,l])=>{const d=l.filter(e=>fracOf('ep:'+e.id)>=.93).length;
      return'<button class="sn press '+(s===cur?'on':'')+'" data-s="'+esc(s)+'"><span class="n">'+esc(s)+'</span><small>'+t('season')+'<br>'+l.length+' '+t('ep')+'</small><div class="pgm"><i style="width:'+(l.length?d/l.length*100:0)+'%"></i></div></button>'}).join('')+'</div>':'')+
      '<div class="sw"><span>'+t('season')+' '+esc(cur)+'</span><span>'+done+' / '+list.length+' '+t('watched')+'</span><div class="bar"><i style="width:'+(list.length?done/list.length*100:0)+'%"></i></div></div>'+
      '<div class="eps" id="eps">'+list.map((e,n)=>{
        const f=fracOf('ep:'+e.id);const th=imgUrl(e.img)||imgUrl(it.img);
        return'<button class="ec press" data-ei="'+n+'"><div class="th">'+(th?'<img src="'+esc(th)+'" loading="lazy" alt="" onload="this.classList.add(\'ld\')" onerror="this.remove()">':'')+'<span class="en">'+e.num+'</span><div class="pp"><span>'+I.play+'</span></div>'+
          (n===ni&&f<.93?'<span class="ud">'+(f>.01?t('cres'):t('upnext'))+'</span>':'')+(f>=.93?'<span class="ck">'+I.check+'</span>':'')+(f>.01&&f<.93?'<div class="pg"><i style="width:'+Math.round(f*100)+'%"></i></div>':'')+'</div><b>'+esc(e.title||(t('ep')+' '+e.num))+'</b><small>'+esc(e.plot||e.dur||'')+'</small></button>'}).join('')+'</div>';
    $$('.sn',box).forEach(b=>b.onclick=()=>{cur=b.dataset.s;store.set('lastsea:'+it.id,cur);draw()});
    $$('.ec',box).forEach(b=>b.onclick=()=>playEp(cur,+b.dataset.ei));
    const eps=$('#eps');const tgt=eps&&eps.children[ni];if(tgt&&ni>0&&eps.scrollTo){const rtl=getComputedStyle(eps).direction==='rtl';const off=tgt.offsetLeft-eps.offsetLeft-16;eps.scrollLeft=rtl?-(eps.scrollWidth-eps.clientWidth)+(eps.scrollWidth-tgt.offsetLeft-tgt.offsetWidth-16)*0:eps.scrollLeft;try{tgt.scrollIntoView({inline:'center',block:'nearest'})}catch(e){}}
    const nl=(seasons.find(x=>x[0]===cur)||[0,[]])[1];const ne=nl[ni];
    const b=$('#dplay');if(b){$('#dplt').textContent=(fracOf('ep:'+(ne&&ne.id))>.01||ni>0?t('resume'):t('play'))+(ne?' · '+t('season').slice(0,lang==='ar'?3:1)+cur+' '+(lang==='ar'?'ح':'E')+ne.num:'');b.onclick=()=>playEp(cur,ni)}
  };
  draw();
}
function startEpisodeQueue(it,j,seasons,s,idx){
  const list=(seasons.find(x=>x[0]===s)||[0,[]])[1];
  // قائمة تشغيل عبر المواسم: الحلقات التالية في الموسم ثم المواسم اللاحقة
  const flat=[];seasons.forEach(([ss,l])=>l.forEach(e=>flat.push({s:ss,e})));
  const at=flat.findIndex(x=>x.s===s&&x.e.id===list[idx].id);
  const cur=flat[at];
  const mk=n=>{const x=flat[n];if(!x)return null;return{title:it.name+' · '+(lang==='ar'?'م'+x.s+' ح'+x.e.num:'S'+x.s+' E'+x.e.num)+(x.e.title?' — '+x.e.title:''),key:'ep:'+x.e.id,urls:srcList('series',x.e.id,x.e.ext),
    item:{k:'series',id:it.id,name:it.name,img:it.img,ext:'',ep:{s:x.s,id:x.e.id}},poster:x.e.img||it.img}};
  const p=getPr()['ep:'+cur.e.id];const res=p&&p.t>10&&p.d&&p.t<p.d*.93;
  startPlayer(Object.assign(mk(at),{live:false,resumeAt:res?p.t:0,queue:{mk,at,len:flat.length}}));
}

/* ================= المشغّل ================= */
const V=$('#v');let P=null;let hls=null;let uiTimer=0;let subUrl='';let subCues=null;let subDelay=0;
const fmt=s=>{if(!isFinite(s)||s<0)s=0;s=Math.floor(s);const h=Math.floor(s/3600),m=Math.floor(s%3600/60),x=s%60;return(h?h+':'+String(m).padStart(2,'0'):m)+':'+String(x).padStart(2,'0')};
function showUi(stay){
  const pl=$('#pl');pl.classList.add('ui');clearTimeout(uiTimer);
  if(!stay&&!V.paused&&!P.drag)uiTimer=setTimeout(()=>pl.classList.remove('ui'),3800);
}
function hideUi(){$('#pl').classList.remove('ui')}
function setPlayIcon(){const p=$('#p-pp');if(p)p.innerHTML=V.paused?I.play:I.pause}
function perr(msg,retry){
  const e=$('#p-err');e.innerHTML='<div>'+msg+'</div>'+(retry?'<button class="bp press" id="p-retry" style="height:42px;font-size:15px">'+t('retry')+'</button>':'');e.classList.remove('hide');$('#p-spin').classList.add('hide');
  const r=$('#p-retry');if(r)r.onclick=()=>{e.classList.add('hide');loadSrc(true)};
}
function showExternal(msg){
  if(!P)return;
  const e=$('#p-err');$('#p-spin').classList.add('hide');
  const ar=lang==='ar';
  e.innerHTML='<div>'+msg+'</div><div class="xb">'+
    '<button class="bp press" data-x="vlc">▶ '+(ar?'تشغيل في VLC':'Play in VLC')+'</button>'+
    '<button class="bs press" data-x="infuse">'+(ar?'تشغيل في Infuse':'Play in Infuse')+'</button>'+
    '<button class="bs press" data-x="copy">'+(ar?'نسخ الرابط':'Copy link')+'</button>'+
    '<button class="bs press" data-x="retry">'+t('retry')+'</button></div>'+
    '<small style="color:var(--dim)">'+(ar?'ثبّت VLC أو Infuse مجانًا من App Store، فهما يشغّلان MKV وكل الصيغ.':'Install VLC or Infuse from the App Store; they play MKV and every format.')+'</small>';
  e.classList.remove('hide');
}
async function directLink(){
  const m=/\/s\/(movie|series)\/([^/.]+)\.([a-z0-9]+)$/i.exec(P.urls[0]);
  if(!m)throw new Error('no');
  const r=await fetch('/link?kind='+m[1]+'&id='+m[2]+'&ext='+encodeURIComponent(m[3]));
  const j=await r.json();if(!j.ok)throw new Error('no');return j.url;
}
$('#p-err').addEventListener('click',async ev=>{
  const b=ev.target.closest('[data-x]');if(!b||!P)return;ev.stopPropagation();
  const x=b.dataset.x;
  if(x==='retry'){$('#p-err').classList.add('hide');P.ui=0;P.retries=0;P.forceHere=true;return loadSrc(true)}
  let url;try{url=await directLink()}catch(e){return toast(lang==='ar'?'تعذّر تجهيز الرابط.':'Could not prepare the link.')}
  if(x==='vlc')location.href='vlc-x-callback://x-callback-url/stream?url='+encodeURIComponent(url);
  else if(x==='infuse')location.href='infuse://x-callback-url/play?url='+encodeURIComponent(url);
  else if(x==='copy'){try{await navigator.clipboard.writeText(url);toast(lang==='ar'?'تم نسخ الرابط':'Link copied')}catch(e){prompt(lang==='ar'?'انسخ الرابط:':'Copy the link:',url)}}
});
function loadHlsLib(){return new Promise((res,rej)=>{if(window.Hls)return res();const s=document.createElement('script');s.src='https://cdnjs.cloudflare.com/ajax/libs/hls.js/1.5.13/hls.min.js';s.onload=res;s.onerror=rej;document.head.appendChild(s)})}
function teardown(){
  if(hls){try{hls.destroy()}catch(e){}hls=null}
  try{V.pause()}catch(e){}V.removeAttribute('src');$$('track',V).forEach(x=>x.remove());try{V.load()}catch(e){}
}
async function loadSrc(retry){
  if(!P)return;
  const url=P.urls[P.ui];
  $('#p-err').classList.add('hide');$('#p-spin').classList.remove('hide');
  teardown();
  const isHls=/\.m3u8(\?|$)/i.test(url);
  const mySeq=++P.seq;
  if(isHls&&!V.canPlayType('application/vnd.apple.mpegurl')){
    try{await loadHlsLib()}catch(e){return perr('تعذّر تحميل مكوّن التشغيل (يحتاج إنترنت).',true)}
    if(mySeq!==P.seq)return;
    if(!window.Hls||!Hls.isSupported())return perr('هذا المتصفح لا يدعم البث المباشر.');
    hls=new Hls({maxBufferLength:30,liveSyncDurationCount:3,enableWorker:true,lowLatencyMode:false});
    hls.on(Hls.Events.ERROR,(_,d)=>{
      if(!d.fatal)return;
      if(d.type===Hls.ErrorTypes.NETWORK_ERROR&&P.retries++<4){setTimeout(()=>{try{hls&&hls.startLoad()}catch(e){}},1200*P.retries)}
      else if(d.type===Hls.ErrorTypes.MEDIA_ERROR&&P.retries++<4){try{hls.recoverMediaError()}catch(e){}}
      else onFail();
    });
    hls.on(Hls.Events.MANIFEST_PARSED,()=>{P.hlsReady=true});
    hls.loadSource(url);hls.attachMedia(V);
  }else{V.src=url}
  V.playbackRate=P.rate||1;
  const pr=V.play();if(pr&&pr.catch)pr.catch(()=>{showUi(true);$('#p-spin').classList.add('hide')});
}
function onFail(){
  if(!P)return;
  if(P.ui+1<P.urls.length){P.ui++;P.retries=0;return loadSrc()}   // جرّب امتدادًا آخر (mp4 / m3u8)
  const c=V.error&&V.error.code;
  if(P.live){if(P.retries++<3){setTimeout(()=>loadSrc(),1800);return}return perr(t('livefail'),true)}
  showExternal(c===4||c===3?t('unsupported'):t('vidfail'));
}
function startPlayer(o){
  teardown();
  P=Object.assign({ui:0,seq:0,retries:0,rate:1,drag:false,stall:0,lastT:0},o,{urls:o.urls});
  const pl=$('#pl');pl.classList.remove('hide');document.body.style.overflow='hidden';
  $('#p-title').textContent=o.title;
  const live=!!o.live;
  $('#p-seekrow').classList.toggle('hide',live);$('#p-livetag').classList.toggle('hide',!live);
  $$('.vodonly').forEach(e=>e.classList.toggle('hide',live));
  $$('.liveonly').forEach(e=>e.classList.toggle('hide',!live||!(o.list&&o.list.length>1)));
  $('#p-now').classList.add('hide');
  $('#p-err').classList.add('hide');
  $('#p-un').classList.add('hide');
  subUrl='';subCues=null;subDelay=0;
  pl.style.setProperty('--ac',ST.hue[imgUrl(o.poster||'')]||'#8b5cf6');
  posterColor(o.poster||'',o.title).then(c=>pl.style.setProperty('--ac',c));
  setPlayIcon();showUi(true);
  pushLayer(()=>{finishPlayer()});
  if(o.resumeAt>0){V.addEventListener('loadedmetadata',()=>{try{V.currentTime=o.resumeAt}catch(e){}},{once:true})}
  if(live)liveEpg();
  try{
    if('mediaSession' in navigator){
      navigator.mediaSession.metadata=new MediaMetadata({title:o.title,artist:'Ayham Player',artwork:o.poster&&imgUrl(o.poster).startsWith('http')?[{src:imgUrl(o.poster)}]:(o.poster?[{src:new URL(imgUrl(o.poster),location.href).href}]:[])});
      navigator.mediaSession.setActionHandler('play',()=>V.play());navigator.mediaSession.setActionHandler('pause',()=>V.pause());
      navigator.mediaSession.setActionHandler('seekbackward',()=>seekBy(-10));navigator.mediaSession.setActionHandler('seekforward',()=>seekBy(10));
      navigator.mediaSession.setActionHandler('nexttrack',o.queue&&o.queue.at+1<o.queue.len?()=>nextEp():(o.list&&o.list.length>1?()=>liveStep(1):null));
      navigator.mediaSession.setActionHandler('previoustrack',o.list&&o.list.length>1?()=>liveStep(-1):null);
    }
  }catch(e){}
  const ext0=((P.urls[0]||'').match(/\.([a-z0-9]+)$/i)||[])[1]||'';
  if(!live&&!/^(mp4|m4v|mov|m3u8)$/i.test(ext0)){
    showExternal((lang==='ar'?'هذا العمل بصيغة ':'This title is in ')+ext0.toUpperCase()+(lang==='ar'?' ولا يشغّلها سفاري على الآيفون مباشرة. شغّله في تطبيق VLC أو Infuse، أو جرّب هنا.':' which Safari on iPhone cannot play directly. Open it in VLC or Infuse, or try here.'));
    return;
  }
  loadSrc();
}
function seekBy(d){if(!P||P.live||!isFinite(V.duration))return;V.currentTime=Math.max(0,Math.min(V.duration-1,V.currentTime+d))}
function savePos(force){
  if(!P||P.live||!P.key||!isFinite(V.duration)||V.duration<=0)return;
  const t0=V.currentTime;if(!force&&Math.abs(t0-P.lastSaved||0)<3)return;P.lastSaved=t0;
  savePrEntry({key:P.key,t:t0,d:V.duration,at:now(),item:P.item,title:P.title});
  if(P.item&&P.item.ep)store.set('lastep:'+P.item.id,P.key);
}
function finishPlayer(){
  savePos(true);clearTimeout(uiTimer);
  if(P)P.seq++;
  teardown();const pl=$('#pl');pl.classList.add('hide');pl.classList.remove('ui');P=null;
  document.body.style.overflow=layers.length?'hidden':'';
  try{if(document.fullscreenElement)document.exitFullscreen()}catch(e){}
  if(ST.tab==='fav')viewFav();
  else if(ST.tab==='home'&&!$('#sheet').classList.contains('hide')===false)refreshCont();
  // تحديث شارات الحلقات في صفحة المسلسل إن كانت مفتوحة
  const sh=$('#sheet');if(!sh.classList.contains('hide')&&sh._redraw)sh._redraw();
}
function refreshCont(){/* الرئيسية تُعاد رسمها عند العودة لها */}
async function liveEpg(){
  if(!P||!P.live)return;
  const it=P.list[P.i];const d=await epgFor(it.id);if(!P||P.list[P.i]!==it)return;
  const el=$('#p-now');if(!d.length){el.classList.add('hide');return}
  const n=Math.floor(now()/1000);const cur=d.find(x=>x.s<=n&&x.e>n)||d[0];const nx=d.find(x=>x.s>=cur.e&&x!==cur)||d[1];
  el.innerHTML='<b>'+t('now')+': '+esc(cur.title)+'</b>'+(nx?'<br><span style="color:var(--dim)">'+t('next')+': '+esc(nx.title)+'</span>':'');el.classList.remove('hide');
}
function playLive(it,list){
  list=list&&list.length?list:[it];let i=list.findIndex(x=>x.id===it.id);if(i<0){list=[it];i=0}
  startLive(list,i);
}
function startLive(list,i){
  const it=list[i];
  startPlayer({title:it.name,key:'',live:true,urls:['/s/live/'+encodeURIComponent(it.id)+'.m3u8'],list,i,item:{k:'live',id:it.id,name:it.name,img:it.img},poster:it.img});
}
function liveStep(d){if(!P||!P.live)return;const n=(P.i+d+P.list.length)%P.list.length;const l=P.list;teardown();P.seq++;startLive(l,n);layers.pop();try{history.back()}catch(e){}}
function nextEp(){
  if(!P||!P.queue)return;const q=P.queue;if(q.at+1>=q.len)return;
  savePos(true);const nx=q.mk(q.at+1);const queue={mk:q.mk,at:q.at+1,len:q.len};
  teardown();P.seq++;
  const sh=$('#sheet');
  P=null;
  startPlayerReplace(Object.assign(nx,{live:false,resumeAt:0,queue}));
}
function startPlayerReplace(o){
  // يستبدل التشغيل الحالي دون إضافة طبقة جديدة في السجل
  const keep=layers.length;
  startPlayer(o);
  // startPlayer دفعت طبقة جديدة؛ نزيل القديمة الزائدة بإبقاء واحدة فقط
  if(layers.length>keep){const top=layers.pop();layers.pop();layers.push(top);try{history.back();history.pushState({l:layers.length},'')}catch(e){}}
}
/* أحداث الفيديو */
V.addEventListener('playing',()=>{$('#p-spin').classList.add('hide');$('#p-err').classList.add('hide');if(P){P.retries=0;P.stall=0}setPlayIcon();showUi()});
V.addEventListener('pause',()=>{setPlayIcon();if(P)showUi(true);savePos(true)});
V.addEventListener('play',()=>{setPlayIcon()});
V.addEventListener('waiting',()=>{$('#p-spin').classList.remove('hide')});
V.addEventListener('canplay',()=>{$('#p-spin').classList.add('hide')});
V.addEventListener('error',()=>{if(P)onFail()});
V.addEventListener('ended',()=>{if(!P)return;savePos(true);if(P.queue&&P.queue.at+1<P.queue.len)nextEp();else{setPlayIcon();showUi(true)}});
V.addEventListener('timeupdate',()=>{
  if(!P)return;
  const cur=V.currentTime,d=V.duration;
  if(!P.live&&isFinite(d)&&d>0){
    if(!P.drag){$('#p-cur').textContent=fmt(cur);$('#p-dur').textContent=fmt(d);
      $('#p-pr').style.width=(cur/d*100)+'%';$('#p-kn').style.left=(cur/d*100)+'%'}
    try{const b=V.buffered;if(b.length){$('#p-bf').style.width=(b.end(b.length-1)/d*100)+'%'}}catch(e){}
    if(now()-(P.saveAt||0)>8000){P.saveAt=now();savePos()}
    const rem=d-cur;const nu=$('#p-un');
    if(P.queue&&P.queue.at+1<P.queue.len&&rem<=25&&rem>1){if(nu.classList.contains('hide')){nu.classList.remove('hide');nu.innerHTML='<span>'+t('nextep')+'</span><button data-a="nextep">'+I.next.replace('<svg','<svg style="width:16px;height:16px;fill:#16092e;vertical-align:middle"')+'</button>'}}
    else nu.classList.add('hide');
  }
  if(subCues)applyCues();
});
/* حارس الانقطاع للبث المباشر */
setInterval(()=>{
  if(!P||!P.live||V.paused)return;
  if(V.currentTime===P.lastT){P.stall++;if(P.stall>=8){P.stall=0;P.retries=0;loadSrc()}}else{P.stall=0;P.lastT=V.currentTime}
},2000);
document.addEventListener('visibilitychange',()=>{if(document.hidden)savePos(true)});
window.addEventListener('pagehide',()=>savePos(true));

/* لمس المشغّل: نقرة = إظهار/إخفاء، نقرتان جانبيًا = ±10ث */
(()=>{
  const pl=$('#pl');let lastTap=0,tapT=0;
  $('#p-tap').addEventListener('click',ev=>{
    if(!P)return;const n=now();const r=pl.getBoundingClientRect();const x=ev.clientX-r.left;const side=x<r.width/3?-1:x>r.width*2/3?1:0;
    if(n-lastTap<300&&side&&!P.live){clearTimeout(tapT);seekBy(side*10);const el=$(side<0?'.rip.l':'.rip.r');el.classList.remove('go');el.offsetWidth;el.classList.add('go');lastTap=0;return}
    lastTap=n;clearTimeout(tapT);
    tapT=setTimeout(()=>{if(pl.classList.contains('ui'))hideUi();else showUi()},side&&!P.live?260:0);
  });
  // شريط التقدم
  const sb=$('#p-sb');
  const frac=ev=>{const r=sb.getBoundingClientRect();return Math.max(0,Math.min(1,(ev.clientX-r.left)/r.width))};
  const move=ev=>{if(!P||!P.drag)return;const f=frac(ev);const d=V.duration;if(!isFinite(d))return;P.dragF=f;$('#p-pr').style.width=f*100+'%';$('#p-kn').style.left=f*100+'%';$('#p-cur').textContent=fmt(f*d)};
  sb.addEventListener('pointerdown',ev=>{if(!P||P.live)return;P.drag=true;sb.classList.add('dr');sb.setPointerCapture(ev.pointerId);clearTimeout(uiTimer);move(ev)});
  sb.addEventListener('pointermove',move);
  const up=()=>{if(!P||!P.drag)return;P.drag=false;sb.classList.remove('dr');if(isFinite(V.duration))V.currentTime=P.dragF*V.duration;showUi()};
  sb.addEventListener('pointerup',up);sb.addEventListener('pointercancel',up);
})();
$('#pl').addEventListener('click',e=>{
  const b=e.target.closest('[data-p]');if(!b||!P)return;e.stopPropagation();
  const a=b.dataset.p;showUi();
  if(a==='back')return popLayerUI();
  if(a==='pp'){if(V.paused)V.play().catch(()=>{});else V.pause();return}
  if(a==='b10')return seekBy(-10);
  if(a==='f10')return seekBy(10);
  if(a==='prev')return liveStep(-1);
  if(a==='next')return liveStep(1);
  if(a==='speed')return sheetSpeed();
  if(a==='subs')return sheetSubs();
  if(a==='audio')return sheetAudio();
  if(a==='pip')return doPip();
  if(a==='air'){try{V.webkitShowPlaybackTargetPicker()}catch(x){}return}
  if(a==='fs')return doFs();
  if(a==='fav'){const it=P.item;if(it&&it.k!=='live'){const base=(ST.idx[it.k]&&ST.idx[it.k].get(String(it.id)))||it;const on=toggleFav(base);toast(on?t('inl'):t('addl'))}else if(it){const on=toggleFav({k:'live',id:it.id,name:it.name,img:it.img,ext:''});toast(on?t('inl'):t('addl'))}return}
});
function doFs(){
  try{if(V.webkitEnterFullscreen&&!V.requestFullscreen){V.webkitEnterFullscreen();return}}catch(e){}
  try{if(document.fullscreenElement)document.exitFullscreen();else($('#pl').requestFullscreen||V.webkitEnterFullscreen||function(){}).call($('#pl'))}catch(e){try{V.webkitEnterFullscreen()}catch(x){}}
  try{screen.orientation&&screen.orientation.lock&&screen.orientation.lock('landscape').catch(()=>{})}catch(e){}
}
function doPip(){
  try{
    if(document.pictureInPictureEnabled&&V.requestPictureInPicture){if(document.pictureInPictureElement)document.exitPictureInPicture();else V.requestPictureInPicture();return}
    if(V.webkitSupportsPresentationMode&&V.webkitSupportsPresentationMode('picture-in-picture')){V.webkitSetPresentationMode(V.webkitPresentationMode==='picture-in-picture'?'inline':'picture-in-picture');return}
  }catch(e){}
  toast(lang==='ar'?'نافذة عائمة غير مدعومة على هذا الجهاز.':'Picture-in-picture is not supported here.');
}
/* قوائم سفلية */
function openSheet2(html,after){
  const d=document.createElement('div');d.className='sh2';d.innerHTML='<div><div class="gr"></div>'+html+'</div>';
  d.addEventListener('click',e=>{if(e.target===d)close()});document.body.appendChild(d);
  const close=()=>{if(d.isConnected){d.remove()}};
  pushLayer(close);d._close=()=>popLayerUI();
  if(after)after(d);return d;
}
function sheetSpeed(){
  const rs=[.5,.75,1,1.25,1.5,2];
  openSheet2('<h4>'+t('speed')+'</h4>'+rs.map(r=>'<button class="it '+(P.rate===r?'on':'')+'" data-r="'+r+'">'+r+'×</button>').join(''),d=>{
    $$('.it',d).forEach(b=>b.onclick=()=>{P.rate=+b.dataset.r;V.playbackRate=P.rate;$('#p-speedl').textContent=P.rate+'×';d._close()})});
}
function sheetAudio(){
  const items=[];
  if(hls&&hls.audioTracks&&hls.audioTracks.length>1)hls.audioTracks.forEach((a,i)=>items.push({n:a.name||a.lang||('Audio '+(i+1)),on:hls.audioTrack===i,set:()=>{hls.audioTrack=i}}));
  else if(V.audioTracks&&V.audioTracks.length>1)for(let i=0;i<V.audioTracks.length;i++){const a=V.audioTracks[i];items.push({n:a.label||a.language||('Audio '+(i+1)),on:a.enabled,set:()=>{for(let j=0;j<V.audioTracks.length;j++)V.audioTracks[j].enabled=j===i}})}
  if(hls&&hls.levels&&hls.levels.length>1){items.push({n:'— '+(lang==='ar'?'الجودة':'Quality')+' —',sep:1});items.push({n:'Auto',on:hls.currentLevel===-1,set:()=>{hls.currentLevel=-1}});hls.levels.forEach((l,i)=>items.push({n:(l.height||'?')+'p',on:hls.currentLevel===i,set:()=>{hls.currentLevel=i}}))}
  if(!items.length)return toast(lang==='ar'?'لا توجد مسارات صوت إضافية في هذا العمل.':'No extra audio tracks.');
  openSheet2('<h4>'+t('audio')+'</h4>'+items.map((x,i)=>x.sep?'<h4 style="color:var(--dim);font-size:13px">'+esc(x.n)+'</h4>':'<button class="it '+(x.on?'on':'')+'" data-i="'+i+'">'+esc(x.n)+'</button>').join(''),d=>{
    $$('.it',d).forEach(b=>b.onclick=()=>{items[+b.dataset.i].set();d._close()})});
}
/* الترجمة: مسارات داخلية + ملف SRT/VTT من الجهاز مع ضبط التأخير */
function parseSub(txt){
  txt=String(txt||'').replace(/^﻿/,'').replace(/\r/g,'');
  const cues=[];const blocks=txt.split(/\n\s*\n/);
  const ts=s=>{const m=/(?:(\d+):)?(\d+):(\d+)[.,](\d+)/.exec(s);return m?(+(m[1]||0))*3600+(+m[2])*60+(+m[3])+(+('0.'+m[4])):NaN};
  for(const b of blocks){
    const lines=b.split('\n').filter(x=>x.trim()!=='');const ti=lines.findIndex(l=>l.includes('-->'));
    if(ti<0)continue;const [a,c]=lines[ti].split('-->');const s=ts(a),e=ts(c);if(isNaN(s)||isNaN(e))continue;
    cues.push({s,e,text:lines.slice(ti+1).join('\n')});
  }
  return cues;
}
function buildVtt(cues,delay){
  const f=x=>{x=Math.max(0,x+delay);const h=Math.floor(x/3600),m=Math.floor(x%3600/60),s=(x%60).toFixed(3);return String(h).padStart(2,'0')+':'+String(m).padStart(2,'0')+':'+(s.length<6?'0':'')+s};
  return'WEBVTT\n\n'+cues.map((c,i)=>(i+1)+'\n'+f(c.s)+' --> '+f(c.e)+'\n'+c.text).join('\n\n')+'\n';
}
function applySub(){
  $$('track',V).forEach(x=>x.remove());
  if(!subCues)return;
  if(subUrl)URL.revokeObjectURL(subUrl);
  subUrl=URL.createObjectURL(new Blob([buildVtt(subCues,subDelay)],{type:'text/vtt'}));
  const tr=document.createElement('track');tr.kind='subtitles';tr.label='Ayham';tr.srclang='ar';tr.src=subUrl;tr.default=true;V.appendChild(tr);
  const on=()=>{for(const x of V.textTracks)if(x.label==='Ayham')x.mode='showing'};on();setTimeout(on,300);tr.addEventListener('load',on);
}
function applyCues(){}
function sheetSubs(){
  const tracks=[];for(const x of V.textTracks)if(x.kind==='subtitles'||x.kind==='captions')tracks.push(x);
  const html='<h4>'+t('subs')+'</h4><button class="it '+(!tracks.some(x=>x.mode==='showing')?'on':'')+'" data-o="off">'+t('off')+'</button>'+
    tracks.map((x,i)=>'<button class="it '+(x.mode==='showing'?'on':'')+'" data-o="'+i+'">'+esc(x.label||x.language||('Track '+(i+1)))+'</button>').join('')+
    '<button class="it" data-o="file">📄 '+t('file')+'</button>'+
    (subCues?'<div class="stp"><b>'+t('delay')+'</b><button data-dl="-0.5">−</button><span id="dlv">'+subDelay.toFixed(1)+'s</span><button data-dl="0.5">+</button></div>':'');
  openSheet2(html,d=>{
    $$('.it',d).forEach(b=>b.onclick=()=>{
      const o=b.dataset.o;
      if(o==='off'){for(const x of V.textTracks)x.mode='disabled';subCues=null;$$('track',V).forEach(x=>x.remove());d._close()}
      else if(o==='file'){const fi=$('#subfile');fi.value='';fi.onchange=async()=>{const f=fi.files[0];if(!f)return;const txt=await f.text();const c=parseSub(txt);if(!c.length)return toast(lang==='ar'?'تعذّر قراءة ملف الترجمة.':'Could not read the subtitle file.');subCues=c;subDelay=0;for(const x of V.textTracks)x.mode='disabled';applySub();toast(lang==='ar'?'تم تحميل الترجمة':'Subtitles loaded')};d._close();setTimeout(()=>fi.click(),50)}
      else{for(const x of V.textTracks)x.mode='disabled';tracks[+o].mode='showing';subCues=null;d._close()}
    });
    $$('[data-dl]',d).forEach(b=>b.onclick=()=>{subDelay=Math.round((subDelay+ +b.dataset.dl)*10)/10;$('#dlv',d).textContent=subDelay.toFixed(1)+'s';applySub()});
  });
}

/* ================= القائمة + إجراءات ================= */
async function openMenu(){
  let acct='';try{const j=ST.acct||await api('');ST.acct=j;const ex=+j.exp;acct='<div class="it" style="cursor:default"><span>'+t('sub')+'</span><small>'+esc(j.status||'')+(ex?' · '+t('exp')+' '+new Date(ex*1000).toLocaleDateString(lang==='ar'?'ar':'en'):'')+(j.max?' · '+t('conn')+' '+esc(j.max):'')+'</small></div>'}catch(e){}
  openSheet2('<h4>'+t('menu')+'</h4>'+acct+'<button class="it" data-m="lang"><span>'+t('lang')+'</span><small>'+(lang==='ar'?'العربية ⇄ English':'English ⇄ العربية')+'</small></button><button class="it" data-m="setup"><span>'+(lang==='ar'?'تغيير بيانات الاشتراك':'Change subscription details')+'</span></button><button class="it" data-m="refresh"><span>'+t('refresh')+'</span></button><button class="it" data-m="out" style="color:var(--bad)"><span>'+t('logout')+'</span></button>',d=>{
    $$('[data-m]',d).forEach(b=>b.onclick=async()=>{
      const m=b.dataset.m;
      if(m==='lang'){lang=lang==='ar'?'en':'ar';store.set('lang',lang);d._close();applyLang();renderTop();go(ST.tab)}
      else if(m==='setup'){d._close();setupBack=true;showSetup('')}
      else if(m==='refresh'){d._close();await DB.clear();ST.loaded={};ST.data={};ST.idx={};ST.info={};go(ST.tab)}
      else if(m==='out'){d._close();if(confirm(lang==='ar'?'تسجيل الخروج من هذا الجهاز؟':'Sign out of this device?'))logout()}
    });
  });
}

/* ================= أحداث عامة (تفويض) ================= */
function itemOf(s){const[k,id]=[s.slice(0,s.indexOf(':')),s.slice(s.indexOf(':')+1)];return(ST.idx[k]&&ST.idx[k].get(id))||(k==='live'&&{k:'live',id,name:'',img:'',tags:{},rating:0,added:0})||null}
document.addEventListener('click',async ev=>{
  const el=ev.target.closest('[data-a]');if(!el)return;
  const a=el.dataset.a;
  if(el.closest('#pl'))return;
  switch(a){
    case'tab':if(el.dataset.t!==ST.tab||window.scrollY>0){if(el.dataset.t===ST.tab){window.scrollTo({top:0,behavior:'smooth'});return}go(el.dataset.t)}return;
    case'search':return openSearch();
    case'srchx':return popLayerUI();
    case'menu':return openMenu();
    case'open':{const it=itemOf(el.dataset.i);if(!it)return;if($('#srch')&&it.k==='live'){const q=ST.data.live.filter(x=>x.id===it.id);}
      if($('#srch')){document.body.style.overflow='hidden'}
      return openDetail(it)}
    case'hplay':{ev.stopPropagation();const it=itemOf(el.dataset.i);if(!it)return;openDetail(it);if(it.k==='movie'){setTimeout(()=>{const b=$('#dplay');if(b&&b.onclick)b.onclick()},700)}return}
    case'hfav':{ev.stopPropagation();const it=itemOf(el.dataset.i);if(!it)return;const on=toggleFav(it);el.innerHTML=(on?I.check:I.plus)+'<span>'+t('mylist')+'</span>';toast(on?t('inl'):t('addl'));return}
    case'cont':{const p=getPr()[el.dataset.pk];if(!p)return;const it=p.item||{};
      if(it.k==='movie'){startPlayer({title:p.title,key:p.key,live:false,urls:srcList('movie',it.id,it.ext),item:it,resumeAt:p.t,poster:it.img});return}
      const base=itemOf('series:'+it.id);if(base)return openDetail(base);return}
    case'goto':return;
    case'cat':{const k=el.dataset.k,c=el.dataset.c;ST.catSel[k]=c;ST.sort[k]=ST.sort[k]||'new';window.scrollTo(0,0);if(k==='live')viewLive();else viewKind(k);return}
    case'sort':{const k=el.dataset.k;const ms=['new','rated','az'];ST.sort[k]=ms[(ms.indexOf(ST.sort[k])+1)%3];viewKind(k);return}
    case'shx':return popLayerUI();
    case'plot':{const pl=$('#dpl');const open=pl.classList.toggle('open');el.textContent=open?t('less'):t('read');return}
    case'nextep':return nextEp();
    case'a2hx':{store.set('a2h',1);$('.a2h')&&$('.a2h').remove();return}
  }
  if(a&&a.startsWith('goto:')){const k=a.slice(5);go(k)}
});
// "عرض المزيد" في الرئيسية يستخدم data-a="goto:..." فنعالجه هنا أيضًا
document.addEventListener('click',ev=>{const el=ev.target.closest('[data-a^="goto:"]');if(el){ev.stopPropagation();go(el.dataset.a.slice(5))}},true);

if(!V.webkitShowPlaybackTargetPicker){const ab=$('#p-air');if(ab)ab.classList.add('hide')}
/* ================= بدء ================= */
applyLang();
(async()=>{
  try{const r=await fetch('/me');
    if(r.ok){const j=await r.json();if(j.setup)return showSetup('');return startApp()}
    if(r.status===503)return showLogin(t('cfg'));
  }catch(e){}
  showLogin('');
})();
})();
