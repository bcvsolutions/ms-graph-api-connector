# Microsoft Graph API Connector

This connector is using Microsoft Graph API for communication. For more information about the API capabilities see https://docs.microsoft.com/en-us/graph/overview?view=graph-rest-1.0

Supported operations for USER
* GET detail of user
* GET all users
* CREATE new user
* UPDATE existing user
* DELETE user

Supported operations for LICENCE
* ASSIGN new licence to user
* REMOVE licence from user

Assigning/removing of licence is done via UUID. If you need to know the UUID of specific licence, see https://docs.microsoft.com/en-us/azure/active-directory/enterprise-users/licensing-service-plan-reference other option is to display licence detail in Azure portal and you will see UUID in the URL bar in the end.

### Configuration

If you change some configuration you need to perform Test operation so the new config will load into connector and new grapClient instance is created.

**Client ID** - UUID of application which you need to create in Azure portal. See https://docs.microsoft.com/en-us/graph/auth-register-app-v2?view=graph-rest-1.0
Created app need these permission Directory.ReadWrite.All and User.ReadWrite.All
If you want to be able to change password via connector you need to assign role "Password administrator" to app.

**Scopes** - Use this value: https://graph.microsoft.com/.default

**Client secret** - On app detail in Azure portal, tab "Certificates & secrets" create new secret.

**Tenant** - Tenant UUID

**National cloud** - In which cloud is you tenant located (Global, China, Germany, UsGovernment)

**Disable password change after login** - User will be forced to change his password again after he logged in to cloud when he just changed password from IdM. Use this option to disable the forced change.

**Proxy port** - If you want to use some proxy set the port otherwise leave the default value (0) and proxy will be ignored

**Proxy hostname** - Proxy hostname

**Proxy username** - Proxy username - fill this if your proxy need authentication

**Proxy password** - - Proxy password - fill this if your proxy need authentication

### Schema
Connector will generate default schema for object class &#95;&#95;ACCOUNT&#95;&#95;
Currently we are supporting "basic" (String, Integer, Boolean) attributes such as firstname, lastname, ...
Other supported attributes are password object and licence object
