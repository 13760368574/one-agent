package com.xiaone.closedloop.task;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static com.xiaone.closedloop.task.TaskModels.*;

@Service
public class TaskService {
  private final Map<String, Task> store = new ConcurrentHashMap<>();
  private final Map<String, Sinks.Many<String>> streams = new ConcurrentHashMap<>();
  private final Executor executor = Executors.newVirtualThreadPerTaskExecutor();

  public Task create(CreateRequest request) {
    String id = "tsk_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    Task task = new Task(id, "trc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
        request.source(), request.tenantId(), request.operator(), request.goal(),
        request.context() == null ? Map.of() : request.context(),
        request.riskLevel() == null ? Risk.READ_ONLY : request.riskLevel(), Status.RECEIVED, Instant.now(), null, null, null);
    store.put(id, task); streams.put(id, Sinks.many().replay().latest());
    executor.execute(() -> execute(task));
    return task;
  }

  public Task get(String id) { return Optional.ofNullable(store.get(id)).orElseThrow(() -> new NoSuchElementException("task not found")); }
  public Sinks.Many<String> stream(String id) { get(id); return streams.get(id); }

  private void execute(Task initial) {
    Task task = initial;
    try {
      authorize(task); task = update(task, Status.AUTHORIZED, null, null); emit(task, "task.accepted", Map.of("taskId", task.taskId()));
      Map<String,Object> plan = planner(task); task = update(task, Status.PLANNING, plan, null); emit(task, "plan.created", plan);
      task = update(task, Status.EXECUTING, plan, null);
      Map<String,Object> evidence = new LinkedHashMap<>();
      for (String tool : tools(plan)) { emit(task, "tool.started", Map.of("tool", tool)); evidence.put(tool, tool(tool, task)); emit(task, "tool.completed", Map.of("tool", tool, "result", evidence.get(tool))); }
      task = update(task, Status.DELIVERING, plan, evidence); task = update(task, Status.SUCCEEDED, plan, evidence);
      emit(task, "task.completed", Map.of("status", task.status().name(), "result", evidence));
    } catch (SecurityException e) { task = fail(task, Status.REJECTED, "PERMISSION", e.getMessage()); }
      catch (Exception e) { fail(task, Status.FAILED, "UNRECOVERABLE", e.getMessage()); }
  }

  private void authorize(Task t) { if (t.riskLevel() == Risk.PROHIBITED) throw new SecurityException("prohibited action"); }
  private Map<String,Object> planner(Task t) { boolean incident = t.goal().contains("故障") || t.goal().contains("诊断") || t.goal().contains("成功率");
    List<String> tools = incident ? List.of("query_metrics", "search_logs", "rank_hypotheses") : List.of("search_knowledge");
    return Map.of("intent", incident ? "INCIDENT_DIAGNOSIS" : "KNOWLEDGE_QA", "tools", tools, "riskLevel", t.riskLevel().name()); }
  @SuppressWarnings("unchecked") private List<String> tools(Map<String,Object> plan) { return (List<String>) plan.get("tools"); }
  private Map<String,Object> tool(String name, Task t) { return switch (name) {
    case "query_metrics" -> Map.of("baseline", .982, "current", .913, "service", t.context().getOrDefault("service", "payment"));
    case "search_logs" -> Map.of("errorCode", "UPSTREAM_TIMEOUT", "count", 14, "source", "SLS");
    case "rank_hypotheses" -> Map.of("hypothesis", "upstream timeout increased", "confidence", .78, "needsVerification", true);
    default -> Map.of("source", "Elasticsearch", "snippet", "授权知识片段", "version", "demo-v1");
  }; }
  private Task update(Task t, Status s, Map<String,Object> plan, Map<String,Object> result) { Task n = new Task(t.taskId(),t.traceId(),t.source(),t.tenantId(),t.operator(),t.goal(),t.context(),t.riskLevel(),s,t.createdAt(),plan,result,t.error()); store.put(t.taskId(), n); return n; }
  private Task fail(Task t, Status s, String category, String message) { Task n = new Task(t.taskId(),t.traceId(),t.source(),t.tenantId(),t.operator(),t.goal(),t.context(),t.riskLevel(),s,t.createdAt(),t.plan(),t.result(),Map.of("category",category,"message",message)); store.put(t.taskId(), n); emit(n,"task.failed",n.error()); return n; }
  private void emit(Task t, String event, Object data) { streams.get(t.taskId()).tryEmitNext("event: " + event + "\ndata: " + data + "\n\n"); }
}
