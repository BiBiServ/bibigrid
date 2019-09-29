# OpenStack credentials setup
## Instance access SSH keys ##
BiBiGrid creates a One-Time-SSH-Private-Key for each cluster during setup. 
In order to connect to the remote you have to have a valid public SSH key placed locally as well as on the cloud provider server. 
This should be located in your home directory in a hidden *.ssh* folder (~/.ssh/id_rsa.pub).
You can either **create** a new keypair (Option 1) or **import** an existing one (Option 2):  

### Create a Key Pair from OpenStack Dashboard (Option 1) ###  
You can let the OpenStack Horizon Dashboard create a Key Pair for you from **Project** -> **Compute** -> **Key Pairs**.
Take out the public key directly from the created key pair in the dashboard. Save this and the private key downloaded 
in the *~/.ssh* folder as 'id_rsa.pub' and 'id_rsa'. You only need the link to the public key file for configuration.

### Import an existing Key File (Option 2) ###  
If you don't already have an existing SSH Key Pair  
```
> ssh-keygen -t rsa -b 4096
```
creates one in your *~/.ssh* directory (e.g. *id_rsa* and *id_rsa.pub*). 
The '.pub' file contains the public key which you have to import within the horizon Dashboard of OpenStack. 
You can find the option under **Project** -> **Compute** -> **Key Pairs**.  

## API Credentials ##
Now you can either source an OpenStack RC File (Option 1) or create a credentials file to include in the configuration file (Option 2).

### Source the OpenStack RC File v3 (Option 1) ###
Click on your Account symbol on the right upper corner of the Dashboard and download the *OpenStack RC File v3*.
You have to source the RC file in a terminal where later BiBiGrid is executed (replace 'FILE' with the actual file name).  
```
> source FILE.sh
```  

### Set up a credentials file (Option 2) ###
Go to **Project** -> **API Access** -> **View Credentials** to transfer the necessary information manually into your credentials file (example below).
It is recommended to save the credentials file in the *.bibigrid* folder in your home directory. There you can find the keys created during the setup, too.

### Example credentials file:

*configuration.yml*
```
[...]
credentialsFile: ~/.bibigrid/credentials.yml
[...]
```

*credentials.yml*
```
projectName: XXX
username: XXX
password: XXX
endpoint: XXX
domain: XXX
projectDomain: XXX
```
