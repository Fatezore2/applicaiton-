
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



public interface UpdateTaskCompletionMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ExampleConnector,
      UpdateTaskCompletionMutation.Data,
      UpdateTaskCompletionMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val isCompleted: Boolean
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val task_update: TaskKey?
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateTaskCompletion"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateTaskCompletionMutation.ref(
  
    id: java.util.UUID,isCompleted: Boolean,
  
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateTaskCompletionMutation.Data,
    UpdateTaskCompletionMutation.Variables
  > =
  ref(
    
      UpdateTaskCompletionMutation.Variables(
        id=id,isCompleted=isCompleted,
  
      )
    
  )

public suspend fun UpdateTaskCompletionMutation.execute(
  
    id: java.util.UUID,isCompleted: Boolean,
  
  
  ): com.google.firebase.dataconnect.MutationResult<
    UpdateTaskCompletionMutation.Data,
    UpdateTaskCompletionMutation.Variables
  > =
  ref(
    
      id=id,isCompleted=isCompleted,
  
    
  ).execute()


