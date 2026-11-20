package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.UserProfileDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.entities.UserAvatar;
import com.lucasbarros.nitrate.repositories.UserAvatarRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;

/**
 * Foto de perfil. Toda imagem enviada é decodificada e regravada aqui como um JPEG quadrado:
 * o que sai do banco é sempre uma imagem nossa, sem metadados (GPS do celular) e sem
 * arquivos disfarçados de imagem.
 */
@Service
public class AvatarService {

    private static final int LADO = 400; // o maior avatar do site tem 112px; 400 cobre telas 3x
    private static final int LADO_MAX_ORIGINAL = 8000; // acima disso a decodificação gastaria memória demais
    private static final float QUALIDADE = 0.85f;

    @Autowired private UserRepository userRepository;
    @Autowired private UserAvatarRepository avatarRepository;
    @Autowired private UserService userService;

    @Transactional
    public UserProfileDTO salvar(User user, MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) throw new IllegalArgumentException("Escolha uma imagem.");
        byte[] jpeg;
        try {
            jpeg = quadrado(ler(arquivo.getBytes()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler a imagem. Envie um JPG ou PNG.");
        }

        User salvo = userRepository.findById(user.getId()).orElseThrow();
        UserAvatar avatar = avatarRepository.findById(salvo.getId()).orElseGet(() -> new UserAvatar(salvo.getId(), null));
        avatar.setData(jpeg);
        avatarRepository.save(avatar);
        // muda a URL da foto (?v=), então a versão antiga em cache deixa de ser usada
        salvo.setAvatarUpdatedAt(Instant.now());
        return userService.perfil(userRepository.save(salvo));
    }

    @Transactional
    public UserProfileDTO remover(User user) {
        User salvo = userRepository.findById(user.getId()).orElseThrow();
        avatarRepository.deleteById(salvo.getId());
        salvo.setAvatarUpdatedAt(null);
        return userService.perfil(userRepository.save(salvo));
    }

    @Transactional(readOnly = true)
    public byte[] imagem(String username) {
        return userRepository.findFirstByUsernameIgnoreCase(username.trim())
                .flatMap(u -> avatarRepository.findById(u.getId()))
                .map(UserAvatar::getData)
                .orElseThrow(() -> new ResourceNotFoundException("Sem foto de perfil."));
    }

    /** decodifica JPG/PNG/GIF/BMP, conferindo o tamanho antes de alocar os pixels */
    private static BufferedImage ler(byte[] bytes) throws IOException {
        try (ImageInputStream entrada = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> leitores = ImageIO.getImageReaders(entrada);
            if (!leitores.hasNext()) throw new IllegalArgumentException("Formato não suportado. Envie um JPG ou PNG.");
            ImageReader leitor = leitores.next();
            try {
                leitor.setInput(entrada, true, true);
                if (leitor.getWidth(0) > LADO_MAX_ORIGINAL || leitor.getHeight(0) > LADO_MAX_ORIGINAL) {
                    throw new IllegalArgumentException("Imagem grande demais. Use uma de até " + LADO_MAX_ORIGINAL + " px de lado.");
                }
                return leitor.read(0);
            } finally {
                leitor.dispose();
            }
        }
    }

    /** recorta o centro em quadrado, reduz para LADO x LADO e grava em JPEG */
    private static byte[] quadrado(BufferedImage original) throws IOException {
        int lado = Math.min(original.getWidth(), original.getHeight());
        int x = (original.getWidth() - lado) / 2;
        int y = (original.getHeight() - lado) / 2;
        int destino = Math.min(LADO, lado);

        BufferedImage saida = new BufferedImage(destino, destino, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = saida.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE); // PNG com transparência: o JPEG não tem canal alfa
            g.fillRect(0, 0, destino, destino);
            g.drawImage(original, 0, 0, destino, destino, x, y, x + lado, y + lado, null);
        } finally {
            g.dispose();
        }

        ImageWriter escritor = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream saidaStream = ImageIO.createImageOutputStream(bytes)) {
            escritor.setOutput(saidaStream);
            ImageWriteParam parametros = escritor.getDefaultWriteParam();
            parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            parametros.setCompressionQuality(QUALIDADE);
            escritor.write(null, new IIOImage(saida, null, null), parametros);
        } finally {
            escritor.dispose();
        }
        return bytes.toByteArray();
    }
}
