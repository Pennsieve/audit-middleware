# audit-middleware

## Installation

### `Pipfile`

Using `Pipfile` is the preferred method of installation. 

Add the requisite `audit-middleware` requirement to the `[packages]` 
section of the `Pipfile`:

```
[packages]
audit-middleware = "*"
```
then

```
$ pipenv install
```

### `setup.py`

The library supports the typical `setup.py` method of installation:

```bash
$ python setup.py install
```

### Usage:

```python
from audit_middleware import GatewayHost, TraceId, AuditLogger

# ...

trace_id = "..." 
logger = AuditLogger(GATEWAY_HOST)

# log a message directly:
logger.enhance(trace_id, "this is a message")

# log a message using the message builder:
logger.message() \
  .append("key-1", "foo") \
  .append("key-2", "bar") \
  .append("key-1", 99.0) \
  .append("key-2", 5) \
  .log(trace_id)
```

## Testing
Run all unit tests:

```bash
make test
```

## Publishing

Update `__version__` in `audit-middleware/__init__.py`

Ensure you have set `$PENNSIEVE_NEXUS_USER` and `$PENNSIEVE_NEXUS_PW`
to your nexus username and password respectively

```bash
make release
```

If prompted for your username provide your standard nexus username.
