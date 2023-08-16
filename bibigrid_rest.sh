#!/bin/bash
if [ "$#" -eq  "0" ]
  then
     echo "Picked $(expr 2 \* $(nproc --all) + 1) workers (CPU+1)"
     uvicorn bibigrid.core.startup_rest:app --workers $(expr 2 \* $(nproc --all) + 1)
else
     uvicorn bibigrid.core.startup_rest:app --workers #1
fi