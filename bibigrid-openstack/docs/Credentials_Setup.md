# OpenStack credentials setup
## Instance access SSH keys ##
BiBiGrid creates a One-Time-SSH-Private-Key for each cluster during setup. Additionally you can add one or more 
public SSH keys that do not necessarily be avilable on the cloud provider server. 

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
credentialsFile: <HOME>/.bibigrid/credentials.yml
[...]
```

*credentials.yml*
```
username: string                                  # Name of user
password: string                                  # Password set by user
endpoint: string                                  # API endpoint
project: string                                   # project name
projectId: string                                 # project id
userDomain: string                                # user domain name
userDomainId: string                              # user domain id
projectDomain: string                             # project domain name
projectDomainId: string                           # project domain id
```
