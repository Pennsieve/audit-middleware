FROM python:2.7 as build
RUN pip install --upgrade pip
RUN pip install pipenv
WORKDIR /audit_middleware
COPY audit_middleware/ ./audit_middleware/
COPY Makefile Pipfile Pipfile.lock setup.py MANIFEST.in README.md ./
RUN pipenv --two install --dev
RUN python setup.py install
RUN python setup.py bdist_wheel

