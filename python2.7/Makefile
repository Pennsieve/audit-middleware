.PHONY: \
	help \
	ci-test \
	build-test-container \
	test \
	clean \
	clean-min \
	test-install-deps \
	clean-build \
	clean-pyc \
	clean-test clean-docker \
	release \
	ci-release \
	build-release-container \
	dist \
	install \
	release

IMAGE_TAG ?= "latest"

help:
	@echo "make help"
	@echo "make test -- run tests locally"
	@echo "make ci-test -- run containerized tests"
	@echo "make clean -- clean up all artifacts"
	@echo "make clean-min -- clan up non-docker artifacts"
	@echo "make install -- build package into current Python's site-packages"
	@echo "make dist -- create a publishable artifact"
	@echo "make release -- publish artifact to private nexus PyPI repository"
	@echo "make ci-release -- release using container"

ci-test: clean-min build-test-container
	@IMAGE_TAG=${IMAGE_TAG} docker-compose up --exit-code-from=audit_middleware_test audit_middleware_test

build-test-container:
	@IMAGE_TAG=${IMAGE_TAG} docker-compose build audit_middleware_test

test-install-deps:
	pipenv install --dev

test: clean test-install-deps
	pipenv run pytest -s

clean: clean-build clean-pyc clean-test clean-docker

clean-min: clean-build clean-pyc clean-test

clean-build:
	rm -fr build/
	rm -fr dist/
	rm -fr .eggs/
	find . -name '*.egg-info' -exec rm -fr {} +
	find . -name '*.egg' -exec rm -fr {} +

clean-pyc:
	find . -name '*.pyc' -exec rm -f {} +
	find . -name '*.pyo' -exec rm -f {} +
	find . -name '*~' -exec rm -f {} +
	find . -name '__pycache__' -exec rm -fr {} +

clean-test:
	rm -fr .tox/
	rm -f .coverage
	rm -fr htmlcov/
	rm -fr .pytest_cache

clean-docker:
	docker-compose down
	docker-compose rm

install: clean
	echo "Install package to activate python's site-packages"
	pipenv run python setup.py install

dist: install
	echo "Build source and wheel package"
	pipenv run python setup.py bdist_wheel

release: dist
	echo "Releasing to private nexus pypi repository"
	pipenv run twine upload dist/* --repository-url https://nexus.pennsieve.cc/repository/pypi-prod/ -u "${PENNSIEVE_NEXUS_USER}" -p "${PENNSIEVE_NEXUS_PW}"

ci-release: clean-min build-release-container
	@IMAGE_TAG=${IMAGE_TAG} docker-compose up --exit-code-from=audit_middleware audit_middleware

build-release-container:
	@IMAGE_TAG=${IMAGE_TAG} docker-compose build audit_middleware
