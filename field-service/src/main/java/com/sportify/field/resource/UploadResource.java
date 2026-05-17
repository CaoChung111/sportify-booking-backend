package com.sportify.field.resource;

import com.sportify.common.dto.ApiResponse;
import com.sportify.field.service.CloudinaryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.Map;

@Path("/api/v1/upload")
@Tag(name = "Upload", description = "Upload assets")
public class UploadResource {

    @Inject
    CloudinaryService cloudinaryService;

    @POST
    @Path("/image")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Upload field image to Cloudinary")
    public Response uploadFieldImage(@RestForm("file") FileUpload file) {
        String imageUrl = cloudinaryService.uploadFieldImage(file);
        return Response.ok(ApiResponse.success("Image uploaded successfully", Map.of("imageUrl", imageUrl))).build();
    }
}
