## Запуск (Нужны JDK 21 + Maven)

```powershell
mvn clean package
Start-Process powershell -ArgumentList '-NoExit', '-Command', 'mvn -pl eureka-server spring-boot:run'
Start-Process powershell -ArgumentList '-NoExit', '-Command', 'mvn -pl book-service spring-boot:run'
Start-Process powershell -ArgumentList '-NoExit', '-Command', 'mvn -pl user-service spring-boot:run'
Start-Process powershell -ArgumentList '-NoExit', '-Command', 'mvn -pl borrow-service spring-boot:run'
```

## ТЕСТЫ:

```powershell
.\ИНТЕГРАЦИОННЫЕ-ТЕСТЫ.ps1
```