package com.sb.sfrigola_core.domains.favorites.controller;

import com.sb.sfrigola_core.config.web.WebConfig;
import com.sb.sfrigola_core.domains.favorites.exception.FavoriteAlreadyExistsException;
import com.sb.sfrigola_core.domains.favorites.exception.NoFavoriteFoundException;
import com.sb.sfrigola_core.domains.favorites.service.IFavoriteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IFavoriteService favoriteService;

    @Test
    void addFavorite_returns200() throws Exception {
        var recipePublicId = UUID.randomUUID();

        mockMvc.perform(post("/api/favorites/{recipePublicId}", recipePublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Recipe added to favorites"));

        verify(favoriteService).addFavorite(recipePublicId);
    }

    @Test
    void addFavorite_alreadyExists_returns409WithErrorCode() throws Exception {
        var recipePublicId = UUID.randomUUID();
        doThrow(new FavoriteAlreadyExistsException(recipePublicId)).when(favoriteService).addFavorite(recipePublicId);

        mockMvc.perform(post("/api/favorites/{recipePublicId}", recipePublicId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorData.errorMessageMap.FAVORITE_ALREADY_EXISTS").exists());
    }

    @Test
    void removeFavorite_returns200() throws Exception {
        var recipePublicId = UUID.randomUUID();

        mockMvc.perform(delete("/api/favorites/{recipePublicId}", recipePublicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Recipe removed from favorites"));

        verify(favoriteService).removeFavorite(recipePublicId);
    }

    @Test
    void removeFavorite_notFound_returns404WithErrorCode() throws Exception {
        var recipePublicId = UUID.randomUUID();
        doThrow(new NoFavoriteFoundException(recipePublicId)).when(favoriteService).removeFavorite(recipePublicId);

        mockMvc.perform(delete("/api/favorites/{recipePublicId}", recipePublicId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.FAVORITE_NOT_FOUND").exists());
    }
}
