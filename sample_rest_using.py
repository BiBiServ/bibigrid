"""
Example for an application using the REST API
"""
import requests


def validate(cluster_id=None, configuration="test.yml"):
    with open(configuration, 'rb') as file:
        files = {'config_file': (configuration, file)}
        if cluster_id:
            params = {"cluster_id": cluster_id}
        else:
            params = {}
        response = requests.post("http://localhost:8000/bibigrid/validate", files=files, params=params, timeout=20)
    response_data = response.json()
    print(response_data)


def create(cluster_id, configuration="test.yml"):
    with open(configuration, 'rb') as file:
        files = {'config_file': (configuration, file)}
        params = {"cluster_id": cluster_id}
        response = requests.post("http://localhost:8000/bibigrid/create", files=files, params=params, timeout=20)
    response_data = response.json()
    print(response_data)


def terminate(cluster_id, configuration="test.yml"):
    with open(configuration, 'rb') as file:
        files = {'config_file': (file)}
        params = {"cluster_id": cluster_id}
        response = requests.post("http://localhost:8000/bibigrid/terminate", params=params, files=files, timeout=20)
    response_data = response.json()
    print(response_data)


def info(cluster_id):
    params = {"cluster_id": cluster_id, "cloud": "bibiserv"}
    response = requests.get("http://localhost:8000/bibigrid/info", params=params, timeout=20)
    response_data = response.json()
    print(response_data)
