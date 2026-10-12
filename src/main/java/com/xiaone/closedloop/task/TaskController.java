package com.xiaone.closedloop.task;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.NoSuchElementException;
import static com.xiaone.closedloop.task.TaskModels.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
  private final TaskService service;
  public TaskController(TaskService service) { this.service = service; }
  @PostMapping public Task create(@Valid @RequestBody CreateRequest request) { return service.create(request); }
  @GetMapping("/{id}") public Task get(@PathVariable String id) { return service.get(id); }
  @GetMapping(value="/{id}/events", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter events(@PathVariable String id) {
    SseEmitter emitter = new SseEmitter(0L);
    service.stream(id).asFlux().subscribe(value -> { try { emitter.send(value); } catch (IOException e) { emitter.completeWithError(e); } }, emitter::completeWithError, emitter::complete);
    return emitter;
  }
  @ExceptionHandler(NoSuchElementException.class) @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
  public String notFound(NoSuchElementException e) { return e.getMessage(); }
}
