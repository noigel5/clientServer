# client
```
SET JAVA="C:\Users\ <user> \.jdks\openjdk-19.0.1\bin\java.exe"
%JAVA% -jar .\chat-3000.jar "localhost" "8080" "my-secret"
```

# server
```
SET JAVA="C:\Users\ <user> \.jdks\openjdk-19.0.1\bin\java.exe"
%JAVA% -cp .\chat-3000.jar com.github.noigel5.server.Server "8080"
```
