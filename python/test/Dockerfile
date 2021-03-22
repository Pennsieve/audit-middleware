FROM python:3.7 as test
WORKDIR /audit_middleware

RUN pip install --upgrade pip
RUN pip install pipenv

COPY Pipfile Pipfile.lock ./
COPY audit_middleware/ ./audit_middleware/
COPY test/ ./test/

RUN pipenv install --system --dev && pipenv run black ./audit_middleware
