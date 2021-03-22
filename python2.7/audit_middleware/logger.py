import requests
import json
import uuid

class Auditor:
    TRACE_ID_HEADER = "X-Bf-Trace-Id"
    ENHANCE_ENDPOINT = "/logs/enhance"

    def enhance(self, trace_id, payload):
        raise NotImplementedError

    def message(self):
        return MessageBuilder(self)


class MessageBuilder:
    def __init__(self, logger):
        self.logger = logger
        self.contents = {}

    def append(self, key, *message):
        self.contents.setdefault(key, []).extend(message)
        return self

    def log(self, trace_id):
        return self.logger.enhance(trace_id, self.contents)


class AuditLogger(Auditor):
    def __init__(self, host):
        self.host = host

    def enhance(self, trace_id, payload):
        headers = {self.TRACE_ID_HEADER: trace_id, "Content-Type": "application/json"}
        data = json.dumps(payload, cls=UUIDEncoder)

        r = requests.post(
            "https://" + self.host + self.ENHANCE_ENDPOINT + "/" + trace_id,
            headers=headers,
            data=data,
        )
        r.raise_for_status()


class UUIDEncoder(json.JSONEncoder):
    """
    Custom JSON encoder that can handle UUIDs
    """

    def default(self, o):
        if isinstance(o, uuid.UUID):
            return str(o)
        return super().default(o)
