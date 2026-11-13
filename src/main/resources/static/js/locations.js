document.addEventListener('DOMContentLoaded', () => {
  const search = document.getElementById('locationSearch');
  if (search) search.addEventListener('input', () => {
    let shown = 0;
    document.querySelectorAll('.location-card').forEach(card => {
      card.hidden = !card.dataset.filter.toLowerCase().includes(search.value.trim().toLowerCase());
      if (!card.hidden) shown++;
    });
    const empty = document.getElementById('locationSearchEmpty');
    if (empty) empty.hidden = shown > 0;
  });
  const plan = document.getElementById('floorPlan');
  if (!plan) return;
  const rooms = [...plan.querySelectorAll('.location-plan-room-data')].map(el => ({
    id: el.dataset.id, code: el.dataset.code, name: el.dataset.name, type: el.dataset.type,
    x: +el.dataset.x, y: +el.dataset.y, z: +el.dataset.z,
    width: +el.dataset.width, depth: +el.dataset.depth
  })).filter(r => [r.x,r.y,r.z,r.width,r.depth].every(Number.isFinite));
  const NS = 'http://www.w3.org/2000/svg';
  const svg = document.createElementNS(NS, 'svg');
  const unit = 34, margin = 45;
  const minX = Math.floor(Math.min(0, ...rooms.map(r => r.x))) - 2;
  const minY = Math.floor(Math.min(0, ...rooms.map(r => r.y))) - 2;
  const maxX = Math.ceil(Math.max(12, ...rooms.map(r => r.x+r.width))) + 3;
  const maxY = Math.ceil(Math.max(10, ...rooms.map(r => r.y+r.depth))) + 3;
  const w = (maxX-minX)*unit+margin*2, h=(maxY-minY)*unit+margin*2;
  svg.setAttribute('viewBox', `0 0 ${w} ${h}`);
  svg.setAttribute('width', w); svg.setAttribute('height',h);
  svg.setAttribute('aria-label','Editable floor plan');
  const node=(tag,attrs,parent=svg)=>{const e=document.createElementNS(NS,tag); for(const [k,v] of Object.entries(attrs))e.setAttribute(k,v);parent.append(e);return e;};
  const px=x=>margin+(x-minX)*unit, py=y=>margin+(y-minY)*unit;
  for(let x=minX;x<=maxX;x++){node('line',{x1:px(x),x2:px(x),y1:margin,y2:h-margin,class:'grid-line'}); node('text',{x:px(x)+2,y:margin-9,class:'axis-label'}).textContent=x;}
  for(let y=minY;y<=maxY;y++){node('line',{x1:margin,x2:w-margin,y1:py(y),y2:py(y),class:'grid-line'});node('text',{x:5,y:py(y)+4,class:'axis-label'}).textContent=y;}
  const inspector=document.getElementById('layoutInspector'), form=document.getElementById('layoutEditForm');
  const conflict=document.getElementById('layoutConflict'); let selected=null, interaction=null;
  const snap=v=>document.getElementById('layoutSnap').checked?Math.round(v*2)/2:Math.round(v*100)/100;
  const collides=a=>rooms.some(b=>a!==b && a.z<b.z+3 && a.z+3>b.z && a.x<b.x+b.width-1e-6 && a.x+a.width>b.x+1e-6 && a.y<b.y+b.depth-1e-6 && a.y+a.depth>b.y+1e-6);
  const groups=new Map();
  const palette={CORRIDOR:'#d6a651',BATHROOM:'#54aea6',OFFICE:'#ae8fdb',STAIRCASE:'#98a8bb',HOTEL_ROOM:'#6aaddb',BEDROOM:'#6aaddb',KITCHEN:'#db9468',LOBBY:'#7dbd96'};
  function update(r){const g=groups.get(r.id), rect=g.querySelector('rect'), handle=g.querySelector('.resize-handle'), label=g.querySelector('text');
    rect.setAttribute('x',px(r.x));rect.setAttribute('y',py(r.y));rect.setAttribute('width',r.width*unit);rect.setAttribute('height',r.depth*unit);
    handle.setAttribute('x',px(r.x+r.width)-6);handle.setAttribute('y',py(r.y+r.depth)-6);
    label.setAttribute('x',px(r.x+r.width/2));label.setAttribute('y',py(r.y+r.depth/2));
    const bad=collides(r); g.classList.toggle('layout-overlap',bad);if(selected===r){for(const key of ['x','y','width','depth'])form.elements[key].value=r[key];conflict.textContent=bad?'Overlaps another space — save will be rejected.':'';form.querySelector('button').disabled=bad;}
  }
  function select(r){selected=r;inspector.hidden=false;document.getElementById('layoutSelectedName').textContent=`${r.name} (${r.code}) — ${r.type.replaceAll('_',' ')}`;form.action=`/locations/${encodeURIComponent(r.id)}/layout`;groups.forEach((g,id)=>g.classList.toggle('layout-selected',id===r.id));update(r);}
  for(const r of rooms){const g=node('g',{class:'layout-space'});groups.set(r.id,g);
    node('rect',{class:'layout-room',rx:2,fill:palette[r.type]||'#73a9ca'},g);
    const label=node('text',{class:'room-text','text-anchor':'middle','pointer-events':'none'},g);label.textContent=r.code;
    node('rect',{class:'resize-handle',width:12,height:12,rx:2},g);
    node('title',{},g).textContent=`${r.name} (${r.type})`;
    g.addEventListener('pointerdown',e=>{if(e.button!==0)return; e.preventDefault();select(r);const resizing=e.target.classList.contains('resize-handle');
      interaction={r,resizing,x:e.clientX,y:e.clientY,initial:{x:r.x,y:r.y,width:r.width,depth:r.depth}};
      svg.setPointerCapture(e.pointerId);
    });update(r);
  }
  svg.addEventListener('pointermove',e=>{if(!interaction)return;const {r,resizing,initial,x,y}=interaction;
    const rect=svg.getBoundingClientRect();const dx=(e.clientX-x)*w/rect.width/unit,dy=(e.clientY-y)*h/rect.height/unit;
    if(resizing){r.width=Math.max(.5,snap(initial.width+dx));r.depth=Math.max(.5,snap(initial.depth+dy));}
    else {r.x=snap(initial.x+dx);r.y=snap(initial.y+dy);}update(r);
  });
  const end=()=>{interaction=null;};svg.addEventListener('pointerup',end);svg.addEventListener('pointercancel',end);
  for(const key of ['x','y','width','depth'])form.elements[key].addEventListener('input',()=>{if(!selected)return;const v=+form.elements[key].value;if(!Number.isFinite(v)||((key==='width'||key==='depth')&&v<=0))return;selected[key]=v;update(selected);});
  plan.replaceChildren(svg);
});
