import responses
from audit_middleware import AuditLogger
import uuid

GATEWAY_HOST = "test-gateway-host"
ENHANCE_LOG_URI = "https://" + GATEWAY_HOST + "/logs/enhance"
test_trace_id = "1234-5678"


def echo_success(request):
    return (200, request.headers, request.body)


@responses.activate
def test_when_calling_enhance():
    responses.add_callback(
        responses.POST,
        ENHANCE_LOG_URI + "/" + test_trace_id,
        callback=echo_success,
        content_type="application/json",
    )
    logger = AuditLogger(GATEWAY_HOST)
    logger.enhance(test_trace_id, "this is a message")
    assert len(responses.calls) == 1
    assert responses.calls[0].request.url == (ENHANCE_LOG_URI + "/" + test_trace_id)
    assert responses.calls[0].response.text == '"this is a message"'


@responses.activate
def test_when_using_the_log_builder():
    responses.add_callback(
        responses.POST,
        ENHANCE_LOG_URI + "/" + test_trace_id,
        callback=echo_success,
        content_type="application/json",
    )
    logger = AuditLogger(GATEWAY_HOST)
    r = (
        logger.message()
        .append("key-1", "foo")
        .append("key-2", "bar")
        .append("key-1", 99.0)
        .append("key-2", 5)
        .append("list-items", "a", "b", "c")
        .append("records", *["123", "456", "789"])
        .append("list-items", *["d", "e"])
        .log(test_trace_id)
    )
    assert r is None
    assert len(responses.calls) == 1
    assert responses.calls[0].request.url == (ENHANCE_LOG_URI + "/" + test_trace_id)
    expected_json = {
        "key-1": ["foo", 99.0],
        "key-2": ["bar", 5],
        "list-items": ["a", "b", "c", "d", "e"],
        "records": ["123", "456", "789"],
    }
    assert responses.calls[0].response.json() == expected_json


@responses.activate
def test_subsequent_messages_have_fresh_context():
    responses.add_callback(
        responses.POST,
        ENHANCE_LOG_URI + "/" + test_trace_id,
        callback=echo_success,
        content_type="application/json",
    )
    logger = AuditLogger(GATEWAY_HOST)
    (
        logger.message()
        .append("key-1", "foo")
        .log(test_trace_id)
    )
    (
        logger.message()
        .append("key-1", "bar")
        .log(test_trace_id)
    )

    assert len(responses.calls) == 2
    assert responses.calls[0].response.json() == {
        "key-1": ["foo"],
    }
    assert responses.calls[1].response.json() == {
        "key-1": ["bar"],
    }


@responses.activate
def test_can_log_uuid():
    responses.add_callback(
        responses.POST, ENHANCE_LOG_URI + "/" + test_trace_id, callback=echo_success,
    )

    _uuid = uuid.uuid4()
    (
        AuditLogger(GATEWAY_HOST)
        .message()
        .append("uuid", _uuid)
        .log(test_trace_id)
    )
    assert len(responses.calls) == 1
    assert responses.calls[0].response.json() == {"uuid": [str(_uuid)]}
    assert responses.calls[0].request.headers["Content-Type"] == "application/json"
