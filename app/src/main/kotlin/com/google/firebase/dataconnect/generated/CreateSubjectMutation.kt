
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



public interface CreateSubjectMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      ExampleConnector,
      CreateSubjectMutation.Data,
      CreateSubjectMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val name: String,
    val description: com.google.firebase.dataconnect.OptionalVariable<String?>,
    val userId: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID
  ) {
    
    
      
      @kotlin.DslMarker public annotation class BuilderDsl

      @BuilderDsl
      public interface Builder {
        public var name: String
        public var description: String?
        public var userId: java.util.UUID
        
      }

      public companion object {
        @Suppress("NAME_SHADOWING")
        public fun build(
          name: String,userId: java.util.UUID,
          block_: Builder.() -> Unit
        ): Variables {
          var name= name
            var description: com.google.firebase.dataconnect.OptionalVariable<String?> =
                com.google.firebase.dataconnect.OptionalVariable.Undefined
            var userId= userId
            

          return object : Builder {
            override var name: String
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { name = value_ }
              
            override var description: String?
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { description = com.google.firebase.dataconnect.OptionalVariable.Value(value_) }
              
            override var userId: java.util.UUID
              get() = throw UnsupportedOperationException("getting builder values is not supported")
              set(value_) { userId = value_ }
              
            
          }.apply(block_)
          .let {
            Variables(
              name=name,description=description,userId=userId,
            )
          }
        }
      }
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val subject_insert: SubjectKey
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "CreateSubject"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun CreateSubjectMutation.ref(
  
    name: String,userId: java.util.UUID,
  
    block_: CreateSubjectMutation.Variables.Builder.() -> Unit = {}
  
): com.google.firebase.dataconnect.MutationRef<
    CreateSubjectMutation.Data,
    CreateSubjectMutation.Variables
  > =
  ref(
    
      CreateSubjectMutation.Variables.build(
        name=name,userId=userId,
  
    block_
      )
    
  )

public suspend fun CreateSubjectMutation.execute(
  
    name: String,userId: java.util.UUID,
  
    block_: CreateSubjectMutation.Variables.Builder.() -> Unit = {}
  
  ): com.google.firebase.dataconnect.MutationResult<
    CreateSubjectMutation.Data,
    CreateSubjectMutation.Variables
  > =
  ref(
    
      name=name,userId=userId,
  
    block_
    
  ).execute()


