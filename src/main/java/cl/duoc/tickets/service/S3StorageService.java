package cl.duoc.tickets.service;

import cl.duoc.tickets.dto.ArchivoS3;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.Map;

/** Operaciones sobre el bucket de AWS S3: subir, descargar, listar y eliminar. */
@Service
public class S3StorageService {

    private final S3Client s3;
    private final String bucket;

    public S3StorageService(S3Client s3, @Value("${app.s3.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    /** Sube (o sobrescribe, si ya existe la clave) un PDF en S3. */
    public void subir(String clave, byte[] contenido, Map<String, String> metadatos) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(clave)
                        .contentType("application/pdf")
                        .metadata(metadatos)
                        .build(),
                RequestBody.fromBytes(contenido));
    }

    public byte[] descargar(String clave) {
        return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(clave).build()).asByteArray();
    }

    public void eliminar(String clave) {
        s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(clave).build());
    }

    /** Lista los objetos del bucket que empiezan con el prefijo (ej: "evento123/" o "evento123/usuarioX/"). */
    public List<ArchivoS3> listar(String prefijo) {
        ListObjectsV2Request.Builder req = ListObjectsV2Request.builder().bucket(bucket);
        if (prefijo != null && !prefijo.isBlank()) {
            req.prefix(prefijo);
        }
        return s3.listObjectsV2Paginator(req.build()).contents().stream()
                .map(o -> new ArchivoS3(o.key(), o.size(), o.lastModified()))
                .toList();
    }

    public String getBucket() {
        return bucket;
    }
}
