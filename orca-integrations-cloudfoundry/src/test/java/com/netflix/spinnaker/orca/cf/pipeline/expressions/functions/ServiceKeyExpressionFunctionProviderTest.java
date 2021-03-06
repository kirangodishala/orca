/*
 * Copyright 2019 Pivotal, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.orca.cf.pipeline.expressions.functions;

import static com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus.RUNNING;
import static com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus.SUCCEEDED;
import static com.netflix.spinnaker.orca.api.pipeline.models.ExecutionType.PIPELINE;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.netflix.spinnaker.kork.expressions.ExpressionFunctionProvider.FunctionDefinition;
import com.netflix.spinnaker.orca.api.pipeline.models.ExecutionStatus;
import com.netflix.spinnaker.orca.api.pipeline.models.PipelineExecution;
import com.netflix.spinnaker.orca.pipeline.model.PipelineExecutionImpl;
import com.netflix.spinnaker.orca.pipeline.model.StageExecutionImpl;
import java.util.*;
import org.junit.jupiter.api.Test;

class ServiceKeyExpressionFunctionProviderTest {
  @Test
  void getFunctionsShouldReturnOneFunctionWithTheCorrectNameWhichHasTwoParameters() {
    ServiceKeyExpressionFunctionProvider functionProvider =
        new ServiceKeyExpressionFunctionProvider();
    Collection<FunctionDefinition> functionDefinitions =
        functionProvider.getFunctions().getFunctionsDefinitions();

    assertThat(functionDefinitions.size()).isEqualTo(1);
    functionDefinitions.stream()
        .findFirst()
        .ifPresent(
            functionDefinition -> {
              assertThat(functionDefinition.getName()).isEqualTo("cfServiceKey");
              assertThat(functionDefinition.getParameters().get(0).getType())
                  .isEqualTo(PipelineExecution.class);
              assertThat(functionDefinition.getParameters().get(1).getType())
                  .isEqualTo(String.class);
            });
  }

  @Test
  void serviceKeyShouldResolveForValidStageType() {
    String user = "user1";
    String password = "password1";
    String url = "mysql.example.com";
    Map<String, Object> serviceKey =
        new ImmutableMap.Builder<String, Object>()
            .put("username", user)
            .put("password", password)
            .put("url", url)
            .build();
    Map<String, Object> katoTaskMapWithResults =
        createKatoTaskMap(SUCCEEDED, singletonList(singletonMap("serviceKey", serviceKey)));
    Map<String, Object> katoTaskMapWithoutResults = createKatoTaskMap(SUCCEEDED, emptyList());
    Map<String, Object> katoTaskMapRunning = createKatoTaskMap(RUNNING, emptyList());
    PipelineExecutionImpl execution =
        new PipelineExecutionImpl(PIPELINE, "stage-name-1", "application-name");
    Map<String, Object> contextWithServiceKey = createContextMap(katoTaskMapWithResults);
    Map<String, Object> contextWithoutServiceKey = createContextMap(katoTaskMapWithoutResults);
    Map<String, Object> contextWithRunningTask = createContextMap(katoTaskMapRunning);

    StageExecutionImpl stage1 =
        new StageExecutionImpl(
            new PipelineExecutionImpl(PIPELINE, "orca"),
            "createServiceKey",
            "stage-name-1",
            contextWithServiceKey);
    stage1.setStatus(SUCCEEDED);
    StageExecutionImpl stage2 =
        new StageExecutionImpl(
            new PipelineExecutionImpl(PIPELINE, "orca"),
            "deployService",
            "stage-name-2",
            contextWithoutServiceKey);
    stage2.setStatus(SUCCEEDED);
    StageExecutionImpl stage3 =
        new StageExecutionImpl(
            new PipelineExecutionImpl(PIPELINE, "orca"),
            "createServiceKey",
            "stage-name-3",
            contextWithoutServiceKey);
    stage3.setStatus(SUCCEEDED);
    StageExecutionImpl stage4 =
        new StageExecutionImpl(
            new PipelineExecutionImpl(PIPELINE, "orca"),
            "createServiceKey",
            "stage-name-4",
            contextWithRunningTask);
    stage4.setStatus(RUNNING);

    execution.getStages().add(stage1);
    execution.getStages().add(stage2);
    execution.getStages().add(stage3);
    execution.getStages().add(stage4);

    Map<String, Object> expectedServiceKey =
        new ImmutableMap.Builder<String, Object>()
            .put("username", user)
            .put("password", password)
            .put("url", url)
            .build();

    Map<String, Object> resultServiceKey =
        ServiceKeyExpressionFunctionProvider.cfServiceKey(execution, "stage-name-1");

    assertThat(resultServiceKey).isEqualTo(expectedServiceKey);
  }

  @Test
  void serviceKeyShouldReturnEmptyMapForNoResult() {
    Map<String, Object> katoTaskMapWithoutResults =
        new ImmutableMap.Builder<String, Object>()
            .put("id", "task-id")
            .put("status", SUCCEEDED)
            .put("history", emptyList())
            .put("resultObjects", emptyList())
            .build();
    PipelineExecutionImpl execution =
        new PipelineExecutionImpl(PIPELINE, "stage-name-1", "application-name");
    Map<String, Object> contextWithoutServiceKey = createContextMap(katoTaskMapWithoutResults);

    StageExecutionImpl stage =
        new StageExecutionImpl(
            new PipelineExecutionImpl(PIPELINE, "orca"),
            "createServiceKey",
            "stage-name-3",
            contextWithoutServiceKey);
    stage.setStatus(SUCCEEDED);
    execution.getStages().add(stage);

    Map<String, Object> resultServiceKey =
        ServiceKeyExpressionFunctionProvider.cfServiceKey(execution, "stage-name-1");

    assertThat(resultServiceKey).isEqualTo(Collections.emptyMap());
  }

  private Map<String, Object> createKatoTaskMap(
      ExecutionStatus status, List<Map<String, Object>> resultObjects) {
    return new ImmutableMap.Builder<String, Object>()
        .put("id", "task-id")
        .put("status", status)
        .put("history", emptyList())
        .put("resultObjects", resultObjects)
        .build();
  }

  private Map<String, Object> createContextMap(Map<String, Object> katoTaskMap) {
    Map<String, Object> context = new HashMap<>();
    context.put("parentStage", "parent-stage");
    context.put("account", "account-name");
    context.put("credentials", "my-account");
    context.put("region", "org > space");
    context.put("kato.tasks", singletonList(katoTaskMap));
    return context;
  }
}
