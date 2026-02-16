
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


public interface GetGoalsForUserQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      ExampleConnector,
      GetGoalsForUserQuery.Data,
      GetGoalsForUserQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val userId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val goals: List<GoalsItem>
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class GoalsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
    val description: String?,
    val targetValue: Int,
    val targetUnit: String,
    val targetDate: com.google.firebase.dataconnect.LocalDate
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "GetGoalsForUser"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun GetGoalsForUserQuery.ref(
  
    userId: java.util.UUID,
  
  
): com.google.firebase.dataconnect.QueryRef<
    GetGoalsForUserQuery.Data,
    GetGoalsForUserQuery.Variables
  > =
  ref(
    
      GetGoalsForUserQuery.Variables(
        userId=userId,
  
      )
    
  )

public suspend fun GetGoalsForUserQuery.execute(
  
    userId: java.util.UUID,
  
  
  ): com.google.firebase.dataconnect.QueryResult<
    GetGoalsForUserQuery.Data,
    GetGoalsForUserQuery.Variables
  > =
  ref(
    
      userId=userId,
  
    
  ).execute()


  public fun GetGoalsForUserQuery.flow(
    
      userId: java.util.UUID,
  
    
    ): kotlinx.coroutines.flow.Flow<GetGoalsForUserQuery.Data> =
    ref(
        
          userId=userId,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

