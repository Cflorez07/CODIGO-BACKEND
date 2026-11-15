package com.app.priorix.Controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.app.priorix.Service.AdjuntoService;
import com.app.priorix.model.entity.AdjuntoClinico;

@Controller
@RequestMapping("/adjuntos")
public class AdjuntoController {

    @Autowired
    private AdjuntoService adjuntoService;

    @PostMapping("/subir")
    @ResponseBody
    public ResponseEntity<?> subir(@RequestParam("archivo") MultipartFile archivo,
                                   @RequestParam(value = "pacienteId", required = false) String pacienteId,
                                   @RequestParam(value = "triajeId", required = false) String triajeId,
                                   @RequestParam(value = "atencionId", required = false) String atencionId,
                                   @RequestParam(value = "historiaId", required = false) String historiaId,
                                   @RequestParam(value = "descripcion", required = false) String descripcion,
                                   Authentication auth) {
        try {
            String username = auth != null ? auth.getName() : "personal";
            AdjuntoClinico adj = adjuntoService.guardarArchivo(archivo, username, pacienteId, triajeId, atencionId, historiaId, descripcion);

            Map<String, Object> resp = new HashMap<>();
            resp.put("id", adj.getId());
            resp.put("nombre", adj.getNombreOriginal());
            resp.put("tipo", adj.getTipoContenido());
            resp.put("tamano", adj.getTamano());
            resp.put("urlVer", "/adjuntos/" + adj.getId() + "/ver");
            resp.put("urlDescarga", "/adjuntos/" + adj.getId() + "/descargar");
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Error interno al guardar el archivo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/{id}/ver")
    public ResponseEntity<byte[]> ver(@PathVariable String id) {
        AdjuntoClinico adj = adjuntoService.obtenerPorId(id).orElse(null);
        if (adj == null) return ResponseEntity.notFound().build();

        try {
            byte[] bytes = adjuntoService.leerContenido(adj);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(adj.getTipoContenido()));
            headers.setContentDispositionFormData("inline", adj.getNombreOriginal());
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable String id) {
        AdjuntoClinico adj = adjuntoService.obtenerPorId(id).orElse(null);
        if (adj == null) return ResponseEntity.notFound().build();

        try {
            byte[] bytes = adjuntoService.leerContenido(adj);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", adj.getNombreOriginal());
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
