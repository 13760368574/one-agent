package com.xiaone.closedloop.task;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.*;

public final class TaskModels {
  private TaskModels() {}
  public enum Status { RECEIVED, REJECTED, AUTHORIZED, PLANNING, EXECUTING, VERIFYING, DELIVERING, SUCCEEDED, PARTIAL_SUCCESS, FAILED }
  public enum Risk { READ_ONLY, LOW_WRITE, CONTROLLED_WRITE, PROHIBITED }
  public record Operator(@NotBlank String userId, List<String> roles) {}
  public record CreateRequest(@NotBlank String source, @NotBlank String tenantId, Operator operator,
      @NotBlank String goal, Map<String,Object> context, Risk riskLevel) {}
  public record Task(String taskId, String traceId, String source, String tenantId, Operator operator,
      String goal, Map<String,Object> context, Risk riskLevel, Status status, Instant createdAt,
      Map<String,Object> plan, Map<String,Object> result, Map<String,Object> error) {}
}
