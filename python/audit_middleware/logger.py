from dataclasses import dataclass, field
from typing import NewType, Dict, List, ClassVar, Any
import requests
import json
import uuid


TraceId = NewType("TraceId", str)
GatewayHost = NewType("GatewayHost", str)


class Auditor:
    TRACE_ID_HEADER: ClassVar[str] = "X-Bf-Trace-Id"
    ENHANCE_ENDPOINT: ClassVar[str] = "/logs/enhance"

    def enhance(self, trace_id: TraceId, payload: object) -> None:
        raise NotImplementedError

    def message(self) -> "MessageBuilder":
        return MessageBuilder(self)


@dataclass
class MessageBuilder:
    logger: Auditor
    contents: Dict[str, List[object]] = field(default_factory=dict)

    def append(self, key: str, *message: List[object]) -> "MessageBuilder":
        self.contents.setdefault(key, []).extend(message)
        return self

    def log(self, trace_id: TraceId) -> None:
        return self.logger.enhance(trace_id, self.contents)


class AuditLogger(Auditor):
    def __init__(self, host: GatewayHost):
        self.host = host

    def enhance(self, trace_id: TraceId, payload: object) -> None:
        headers = {self.TRACE_ID_HEADER: trace_id, "Content-Type": "application/json"}
        data = json.dumps(payload, cls=UUIDEncoder)

        r = requests.post(
            f"https://{self.host}{self.ENHANCE_ENDPOINT}/{trace_id}",
            headers=headers,
            data=data,
        )
        r.raise_for_status()


class UUIDEncoder(json.JSONEncoder):
    """
    Custom JSON encoder that can handle UUIDs
    """

    def default(self, o: Any) -> Any:
        if isinstance(o, uuid.UUID):
            return str(o)
        return super().default(o)
