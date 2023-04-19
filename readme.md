```./gradlew build``` to build project

The proto files are auto generated in build/generated/source/proto/main/java based off of the protofile 


There is Gradle which builds the project and download dependencies, unrelated to gRPC. Gradle uses the following files:
* build.gradle

There is .proto files which specifies the behaviors of a piece of code used to generate code in language of your choice:
* proto folder