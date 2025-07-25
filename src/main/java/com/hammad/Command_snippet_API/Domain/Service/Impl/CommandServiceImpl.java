package com.hammad.Command_snippet_API.Domain.Service.Impl;

import ch.qos.logback.classic.Logger;
import com.hammad.Command_snippet_API.Domain.Dto.CommandDto;
import com.hammad.Command_snippet_API.Domain.Dto.CommandPageResponse;
import com.hammad.Command_snippet_API.Domain.Entity.Command;
import com.hammad.Command_snippet_API.Domain.Mapper.CommandMapper;
import com.hammad.Command_snippet_API.Domain.Repository.CommandRepository;
import com.hammad.Command_snippet_API.Domain.Service.CommandService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;


@Service
public class CommandServiceImpl implements CommandService {

    private  final CommandRepository commandRepository;
    private  final CommandMapper commandMapper;
    private static final List<String> ALLOWED_SORT_FIELDS = List.of("howTo", "line","platform");

    public CommandServiceImpl(CommandRepository commandRepository,CommandMapper commandMapper){
        this.commandRepository = commandRepository;
        this.commandMapper = commandMapper;
    }

//    Get all paginated commands it handles sorting,filtering and caching too

    @Override
    @Cacheable(value = "commands", key = "#page + '_' + #size + '_' + #sortBy + '_' + #sortDirection + '_' + (#platform != null ? #platform : 'ALL')")
    public CommandPageResponse getAll(int page, int size, String sortBy, String sortDirection, String platform) {
        // Ensure sorting is valid
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field: " + sortBy);
        }
        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        // Create pageable request with sorting
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), sort);

        // Fetch paginated data with optional filtering
        Page<Command> commandPage = (platform == null || platform.isEmpty())
                ? commandRepository.findAll(pageable)  // No filter, get all
                : commandRepository.findByPlatformContainingIgnoreCase(platform, pageable); // Filter by platform

        // Convert entities to DTOs
        List<CommandDto> commandDtos = commandPage.stream()
                .map(commandMapper::toDto)
                .toList();
        System.out.println("Fetching data from database...");
        // Return paginated response
        return new CommandPageResponse(commandDtos, commandPage.getTotalPages(), commandPage.getTotalElements());
    }


//    Create commands service
    @Override
    @CacheEvict(value = "commands", allEntries = true) // Clears the first page
    public Command createCommand(Command command) {
        if (null != command.getId()) {
            throw new IllegalArgumentException("Command has already an id");
        }
        if (null == command.getHowTo() || command.getHowTo().isBlank()) {
            throw new IllegalArgumentException("Command should have a value");
        }
        LocalDateTime now = LocalDateTime.now();

        return commandRepository.save(new Command(
                null,
                command.getHowTo(),
                command.getLine(),
                command.getPlatform(),
                now, now
        ));
    }
// Get a single command by id
    @Override
    public Optional<Command> getCommand(UUID id) {
        return commandRepository.findById(id);
    }
//  Update an existing command by id
    @Override
    @CacheEvict(value = "commands", allEntries = true)
    public Command updateCommand(UUID id, Command command) {
        if(null == command.getId()){
            throw  new IllegalArgumentException("Command must have an id");
        }
        if(!Objects.equals(command.getId(),id)){
            throw  new IllegalArgumentException("Attempting to change command id this is not permitted");
        }
        Command existingCommand  = commandRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Command not found"));

                existingCommand.setHowTo(command.getHowTo());
                existingCommand.setLine(command.getLine());
                existingCommand.setPlatform(command.getPlatform());
                return commandRepository.save(existingCommand);
    }

//    Delete a command by id

    @Override
    @CacheEvict(value = "commands", allEntries = true)
    public void deleteCommand(UUID id) {
        commandRepository.deleteById(id);
    }


}
