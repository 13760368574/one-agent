import React, {useState} from 'react';
import {createRoot} from 'react-dom/client';
import './style.css';

function App(){const [goal,setGoal]=useState('排查最近30分钟支付成功率下降原因');const [events,setEvents]=useState([]);const [task,setTask]=useState(null);
 async function submit(){setEvents([]);const r=await fetch('/api/v1/tasks',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({source:'WEB',tenantId:'tenant_demo',operator:{userId:'user_demo',roles:['OPS']},goal,context:{service:'payment',environment:'prod',timeRange:['start','end']},riskLevel:'READ_ONLY'})});const t=await r.json();setTask(t);const es=new EventSource(`/api/v1/tasks/${t.taskId}/events`);es.onmessage=e=>setEvents(x=>[...x,e.data]);es.onerror=()=>es.close()}
 return <main><h1>小 One 业务闭环</h1><textarea value={goal} onChange={e=>setGoal(e.target.value)}/><button onClick={submit}>创建任务</button>{task&&<p>任务：{task.taskId}</p>}<section>{events.map((e,i)=><pre key={i}>{e}</pre>)}</section></main>}
createRoot(document.getElementById('root')).render(<App/>);
