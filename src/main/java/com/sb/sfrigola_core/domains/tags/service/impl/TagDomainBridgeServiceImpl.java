package com.sb.sfrigola_core.domains.tags.service.impl;

import com.sb.sfrigola_core.domains.tags.entity.Tag;
import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.exception.NoTagFoundException;
import com.sb.sfrigola_core.domains.tags.exception.TagScopeNotAllowedException;
import com.sb.sfrigola_core.domains.tags.repository.ITagRepository;
import com.sb.sfrigola_core.domains.tags.service.ITagDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagDomainBridgeServiceImpl implements ITagDomainBridgeService {

    private final ITagRepository tagRepository;

    @Override
    public List<Tag> getTagsUsableForIngredients(List<UUID> publicIds) {
        return resolveAndValidateScope(publicIds, EnumSet.of(TagScope.INGREDIENT, TagScope.BOTH));
    }

    @Override
    public List<Tag> getTagsUsableForRecipes(List<UUID> publicIds) {
        return resolveAndValidateScope(publicIds, EnumSet.of(TagScope.RECIPE, TagScope.BOTH));
    }

    // =========================================================
    // PRIVATE
    // =========================================================
    private List<Tag> resolveAndValidateScope(List<UUID> publicIds, Set<TagScope> allowedScopes) {
        var tags = tagRepository.findByPublicIdIn(publicIds);

        var foundIds = tags.stream().map(Tag::getPublicId).collect(Collectors.toSet());
        var missingIds = publicIds.stream().filter(id -> !foundIds.contains(id)).toList();
        if (!missingIds.isEmpty()) {
            throw new NoTagFoundException(missingIds);
        }


        tags.forEach(tag -> {
            if (!allowedScopes.contains(tag.getScope())) {
                throw new TagScopeNotAllowedException(tag.getPublicId(), tag.getScope());
            }
        });

        return tags;
    }
}