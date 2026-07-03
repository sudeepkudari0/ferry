import re

path = 'app/src/main/java/com/mobilerun/portal/service/MobilerunAccessibilityService.kt'
with open(path, 'r') as f:
    content = f.read()

# Remove unresolved imports
content = re.sub(r'import com\.mobilerun\.portal\.ui\..*\n', '', content)
content = re.sub(r'import com\.mobilerun\.portal\.api\.ApiResponse\n', '', content)

# Remove actionDispatcher, socketServer, websocketServer declarations
content = re.sub(r'private lateinit var actionDispatcher: ActionDispatcher\n', '', content)
content = re.sub(r'private var socketServer: SocketServer\? = null\n', '', content)
content = re.sub(r'private var websocketServer: PortalWebSocketServer\? = null\n', '', content)

# Remove initialization
content = re.sub(r'actionDispatcher = ActionDispatcher\(apiHandler\)\n', '', content)
content = re.sub(r'socketServer = SocketServer\(apiHandler, configManager, actionDispatcher\)\n', '', content)
content = re.sub(r'ReverseConnectionService\.getInstance\(\)\?.+\n', '', content)
content = re.sub(r'ReverseConnectionService\.requestStart\(this\)\n', '', content)
content = re.sub(r'ReverseConnectionService\.requestStop\(\)\n', '', content)

# Remove updateSocketServerPort
content = re.sub(r'fun updateSocketServerPort\(port: Int\): Boolean \{[\s\S]*?\}\n\n    // Screenshot functionality', '// Screenshot functionality', content)

# Expose apiHandler
content = content.replace('private var apiHandler: ApiHandler? = null', 'var apiHandler: ApiHandler? = null')

with open(path, 'w') as f:
    f.write(content)
