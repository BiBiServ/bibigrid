# after sourcing a venv
uv pip install --all-extras -r pyproject.toml
uv pip compile pyproject.toml > requirements.txt
uv pip compile --extra rest pyproject.toml > requirements-rest.txt
uv pip compile --extra dev pyproject.toml > requirements-dev.txt
uv pip compile --extra rest --extra dev pyproject.toml > requirements-full.txt
uv pip compile --all-extras pyproject.toml > requirements-all.txt
uv lock