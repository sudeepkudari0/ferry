# Add project-specific ProGuard/R8 rules here as they become necessary.
# Keep Gson model classes intact since we serialize/deserialize them via reflection.
-keep class com.example.mobileagent.agent.Action { *; }
-keep class com.example.mobileagent.net.DeviceState { *; }
