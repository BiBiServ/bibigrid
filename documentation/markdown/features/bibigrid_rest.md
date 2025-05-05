# BiBiGrid REST API Documentation

## Overview

The BiBiGrid REST API enables efficient management of cloud clusters through RESTful endpoints. It supports essential
operations such as validating configurations, creating and terminating clusters, and retrieving detailed cluster
information. Developed with FastAPI and Uvicorn, the API ensures high performance and scalability.

## API Endpoints

### `/bibigrid/requirements`

- **Method**: `GET`
- **Description**: Returns the cloud node requirements from the configuration files.

### `/bibigrid/validate`

- **Method**: `POST`
- **Parameters**:
    - `configurations_json` (required)
    - `cluster_id` (optional)
- **Description**: Validates the provided configuration JSON.
  If a `cluster_id` is given, validation logs are stored under that id.

### `/bibigrid/create/`

- **Method**: `POST`
- **Parameters**:
    - `configurations_json` (required)
    - `cluster_id` (optional)
- **Description**: Begins the creation process of a cluster as per the supplied configuration JSON.

### `/bibigrid/terminate/{cluster_id}`

- **Method**: `POST`
- **Parameters**:
    - `configurations_json` (required)
    - `cluster_id` (required)
- **Description**: Initiates termination of the specified cluster.

### `/bibigrid/info/{cluster_id}`

- **Method**: `POST`
- **Parameters**:
    - `cluster_id` (required)
    - `configurations_json` (required)
- **Description**: Fetches detailed information about a specified cluster.

### `/bibigrid/log/{cluster_id}`

- **Method**: `GET`
- **Parameters**:
    - `cluster_id` (required)
    - `lines` (optional)
- **Description**: Retrieves the log for a specified cluster, optionally showing a specific number of last lines.

### `/bibigrid/state/{cluster_id}`

- **Method**: `GET`
- **Parameters**:
    - `cluster_id` (required)
- **Description**: Provides the operational state of a specified cluster, indicating if creation has been completed.

## Logging and Setup

- **Log Directory**: A `log` folder is utilized for storing logs.
- **Logger Initialization**: The logger is created for each cluster and logs are stored with the filename
  following the pattern `{cluster_id}.log`. A general bibigrid rest log file is created as `bibigrid_rest.log`.

## Starting the BiBiGrid REST API

To start the BiBiGrid REST API, you can use a Bash script as follows.

### Execution Instructions

Run the bibigrid_rest.sh script:

   ```
   ./bibigrid_rest.sh
   ```

- You may pass additional options to specify configurations:
  ```
  ./bibigrid_rest.sh -c openstack
  ```

Afterwards you can take a look at the `/docs` section to see the automatically generated documentation and to try it
out.