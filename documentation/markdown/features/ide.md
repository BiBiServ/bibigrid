# Web IDE

Expects `-cid` set and starts [Theia Web IDE](../software/theia_ide.md).

## Port Forwarding
Tries to forward `localhost:9191` to `remote:8181` (Theia listens at `8181`).
In case this port is already in use, a number between 1 and 100 is added to the last attempted port and a new attempt is made.