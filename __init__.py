import nonebot
from fastapi import FastAPI
from urllib.parse import unquote

from pydantic import BaseModel

app: FastAPI = nonebot.get_app()

class MessageSegment(BaseModel):
    type: str
    content: str

class FackFastApi(BaseModel):
    data: list[MessageSegment]


@app.post("/plana")
async def custom_api(messages: FackFastApi):
    for m in messages.data:
        print(m.type)
        print(unquote(m.content))
    print(messages)
    return "ok"