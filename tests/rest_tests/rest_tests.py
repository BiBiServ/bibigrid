import json
import logging
import os
import time

import yaml
from fastapi.testclient import TestClient

from bibigrid.core.startup_rest import app
from bibigrid.core.utility.paths.basic_path import ROOT_PATH, CLOUD_NODE_REQUIREMENTS_PATH

logging.basicConfig(level=logging.INFO)

with open(os.path.join(ROOT_PATH, "resources/tests/rest_test.json"), 'r', encoding='utf-8') as file:
    configurations_json = json.load(file)

CLUSTER_ID = "123456789123456"

client = TestClient(app)
# Read the cloud_node_requirements YAML file and load it into a dictionary

with open(CLOUD_NODE_REQUIREMENTS_PATH, "r", encoding="utf-8") as cloud_node_requirements_file:
    cloud_node_requirements = yaml.safe_load(cloud_node_requirements_file)


def test_get_requirements():
    response = client.get("/bibigrid/requirements")

    # Assert the response status code is 200
    assert response.status_code == 200

    # Assert the structure of the returned JSON content
    assert response.json() == {"cloud_node_requirements": cloud_node_requirements}
    print(f"Response: {response.json()}")


def test_validate():
    response = client.post(f"/bibigrid/validate?cluster_id={CLUSTER_ID}", json=configurations_json)
    assert response.status_code == 200
    response_data = response.json()
    assert response_data["success"] is True
    assert "cluster_id" in response_data
    print(f"Response: {response_data}")


def test_create():
    response = client.post(f"/bibigrid/create?cluster_id={CLUSTER_ID}", json=configurations_json)
    assert response.status_code == 202  # Accepted
    response_data = response.json()
    assert "cluster_id" in response_data
    print(f"Response: {response_data}")


def test_terminate_cluster():
    response = client.post(f"/bibigrid/terminate/{CLUSTER_ID}", json=configurations_json)
    assert response.status_code == 202
    response_data = response.json()
    assert response_data["message"] == "Termination successfully requested."
    print(f"Response: {response_data}")


def test_info():
    response = client.post(f"/bibigrid/info/{CLUSTER_ID}", json=configurations_json)
    assert response.status_code == 200
    response_data = response.json()
    assert "message" in response_data
    print(f"Response: {response_data}")


def test_get_log():
    response = client.get(f"/bibigrid/log/{CLUSTER_ID}")
    assert response.status_code == 200  # Depending on whether the log exists
    response_data = response.json()
    assert "log" in response_data
    print(f"Response: {response_data}")


def test_state(state, do_assert=False):
    response = client.get(f"/bibigrid/state/{CLUSTER_ID}")
    response_data = response.json()
    logging.info(f"Looking for state {state} - Got data {response_data}")
    if do_assert:
        assert response.status_code == 200
        assert "state" in response_data
        assert response_data["state"] == state
    print(response_data)
    return response_data["state"] == state


# Run tests
if __name__ == "__main__":
    start_time = time.time()
    test_get_requirements()
    test_validate()
    test_create()
    test_state("starting", do_assert=True)
    while test_state("starting"):
        time.sleep(10)
    test_state("running", do_assert=True)
    test_terminate_cluster()
    while test_state("running"):
        time.sleep(2)
    test_state("terminated", do_assert=True)
    test_get_log()
    end_time = time.time()
    execution_time = end_time - start_time
    print(f"Execution time: {execution_time / 60} minutes")
