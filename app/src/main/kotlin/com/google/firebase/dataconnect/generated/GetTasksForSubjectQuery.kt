
@file:kotlin.Suppress(
  "KotlinRedundantDiagnosticSuppress",
  "LocalVariableName",
  "MayBeConstant",
  "RedundantVisibilityModifier",
  "RemoveEmptyClassBody",
  "SpellCheckingInspection",
  "LocalVariableName",
  "unused",
)

package com.google.firebase.dataconnect.generated


import kotlinx.coroutines.flow.filterNotNull as _flow_filterNotNull
import kotlinx.coroutines.flow.map as _flow_map


public interface GetTasksForSubjectQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ExampleConnector,
      GetTasksForSubjectQuery.Data,
      GetTasksForSubjectQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val subjectId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val tasks: List<TasksItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class TasksItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val name: String,
    val description: String?,
    val isCompleted: Boolean?
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetTasksForSubject"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun GetTasksForSubjectQuery.ref(
  
    subjectId: java.util.UUID,
  
  
): com.google.firebase.dataconnect.QueryRef<
    GetTasksForSubjectQuery.Data,
    GetTasksForSubjectQuery.Variables
  > =
  ref(
    
      GetTasksForSubjectQuery.Variables(
        subjectId=subjectId,
  
      )
    
  )

public suspend fun GetTasksForSubjectQuery.execute(
  
    subjectId: java.util.UUID,
  
  
  ): com.google.firebase.dataconnect.QueryResult<
    GetTasksForSubjectQuery.Data,
    GetTasksForSubjectQuery.Variables
  > =
  ref(
    
      subjectId=subjectId,
  
    
  ).execute()


  public fun GetTasksForSubjectQuery.flow(
    
      subjectId: java.util.UUID,
  
    
    ): kotlinx.coroutines.flow.Flow<GetTasksForSubjectQuery.Data> =
    ref(
        
          subjectId=subjectId,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

