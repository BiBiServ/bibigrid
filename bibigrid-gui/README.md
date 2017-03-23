# bibigrid-gui

A browser based graphical user interface for the BiBiGrid project. (First working version)

---
**Requirements:**

- Java 
- Maven
- Node Package Manager (npm)

---
**Quick installation guide:**

1. clone this repository
2. go to the ```/bibigrid-gui/src/main/resources/public``` subfolder
3. get all necessary packages & typings by running: ```npm install```
 - development features can be added with: ```npm install --only=dev``` after having run ```npm install```
4. go back to your root-clone folder
5. build your java-package using: ```mvn clean package ```

If all went well, you should now have a ```BiBiGrid-gui-0.1.0.jar``` file in your ```/bibigrid-gui/target``` folder.
Execute this file using: ```java -jar BiBiGrid-gui-0.1.0.jar```. A spring-boot server starts using port :8080 for communication.
Just type ```http://localhost:8080/``` into the address bar of your favorite browser and you are good to go!

---

If you want more information just visit the main project [here](https://github.com/BiBiServ/bibigrid).
