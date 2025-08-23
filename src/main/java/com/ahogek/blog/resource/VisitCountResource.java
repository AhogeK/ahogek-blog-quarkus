package com.ahogek.blog.resource;

import com.ahogek.blog.service.VisitCountService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

/**
 * 网站访问计数资源
 *
 * @author AhogeK
 * @since 2025-08-21 14:25:30
 */
@Path("/api/visit")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VisitCountResource {

    private final VisitCountService visitCountService;

    public VisitCountResource(VisitCountService visitCountService) {
        this.visitCountService = visitCountService;
    }

    @POST
    @Path("/increment")
    public Response incrementVisit() {
        int newCount = visitCountService.incrementCount();
        return Response.ok(Map.of(
                "count", newCount,
                "timestamp", System.currentTimeMillis()
        )).build();
    }
}
