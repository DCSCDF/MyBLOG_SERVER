package com.jiuliu.myblog_dev.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ServiceResultTest {

    @Test
    void dataCarriesPayloadWithCode200() {
        ServiceResult result = ServiceResult.data("payload");

        assertEquals(200, result.getCode());
        assertEquals("payload", result.getData());
    }

    @Test
    void errorSupportsSetCodeChainingUsedAcrossServices() {
        ServiceResult result = ServiceResult.error("bad request").setCode(400);

        assertEquals(400, result.getCode());
        assertEquals("bad request", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void okWithMessageAndSetMsgChaining() {
        ServiceResult ok = ServiceResult.ok("done");
        assertEquals(200, ok.getCode());
        assertEquals("done", ok.getMsg());

        ServiceResult chained = ServiceResult.ok().setMsg("删除成功");
        assertEquals(200, chained.getCode());
        assertEquals("删除成功", chained.getMsg());
        assertSame(chained, chained.setCode(200));
    }
}
