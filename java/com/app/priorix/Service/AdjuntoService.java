package com.app.priorix.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.app.priorix.Repository.AdjuntoClinicoRepository;
import com.app.priorix.model.entity.AdjuntoClinico;

@Service
public class AdjuntoService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "application/pdf"
    );

    private final Path storageFolder = Paths.get("uploads", "adjuntos").toAbsolutePath().normalize();

    @Autowired
    private AdjuntoClinicoRepository adjuntoRepository;

    public AdjuntoService() {
        try {
            Files.createDirectories(storageFolder);
        } catch (IOException e) {
            System.err.println("No se pudo crear el directorio de adjuntos: " + e.getMessage());
        }
    }

    public AdjuntoClinico guardarArchivo(MultipartFile file, String subidoPor, String pacienteId,
                                        String triajeId, String atencionId, String historiaId, String descripcion) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o no fue enviado.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido de 10 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Formato no permitido. Solo se aceptan imágenes (JPG, PNG, WEBP) o documentos PDF.");
        }

        Files.createDirectories(storageFolder);

        String orig = file.getOriginalFilename();
        String extension = "";
        if (orig != null && orig.contains(".")) {
            extension = orig.substring(orig.lastIndexOf("."));
        }
        String uniqueName = UUID.randomUUID().toString() + extension;
        Path targetPath = storageFolder.resolve(uniqueName);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        AdjuntoClinico adj = new AdjuntoClinico();
        adj.setNombreOriginal(orig != null ? orig : uniqueName);
        adj.setTipoContenido(contentType);
        adj.setTamano(file.getSize());
        adj.setSubidoPor(subidoPor);
        adj.setPacienteId(pacienteId);
        adj.setTriajeId(triajeId);
        adj.setAtencionId(atencionId);
        adj.setHistoriaClinicaId(historiaId);
        adj.setDescripcion(descripcion);
        adj.setRutaArchivo(targetPath.toString());

        return adjuntoRepository.save(adj);
    }

    public List<AdjuntoClinico> listarPorPaciente(String pacienteId) {
        return adjuntoRepository.findByPacienteId(pacienteId);
    }

    public List<AdjuntoClinico> listarPorHistoria(String historiaId) {
        return adjuntoRepository.findByHistoriaClinicaId(historiaId);
    }

    public Optional<AdjuntoClinico> obtenerPorId(String id) {
        return adjuntoRepository.findById(id);
    }

    public byte[] leerContenido(AdjuntoClinico adj) throws IOException {
        Path p = Paths.get(adj.getRutaArchivo());
        if (!Files.exists(p)) {
            throw new IOException("El archivo físico no existe en el servidor.");
        }
        return Files.readAllBytes(p);
    }
}
