package org.yarr.merlionapi2.service;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yarr.merlionapi2.model.Bindings;
import org.yarr.merlionapi2.model.Bond;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BindService
{
    private final Logger log = LoggerFactory.getLogger(BindService.class);
    private final static String STORAGE = "./data/bindings.json";
    private final static ObjectMapper mapper = new ObjectMapper();

    private Bindings bindings = get();

    public Bindings all() {
        return get();
    }

    private synchronized Bindings get() {
        try
        {
            Bindings bindings = mapper.readValue(new File(STORAGE), Bindings.class);
            log.debug("Read {} bonds from '{}' file", bindings.bonds().size(), STORAGE);
            this.bindings = bindings;
        } catch (Exception e) {
            log.error("Cannot read/parse '{}' file - '{}', no binding info available", STORAGE, e.getMessage());
            log.debug("Exception trace", e);
            if (bindings == null) {
                log.debug("Replacing with empty map instead");
                bindings = new Bindings(new HashMap<>());
            }
        }
        return bindings;
    }

    public Set<Bond> get(String catalogId) {
        return get().bonds().get(catalogId);
    }

    private synchronized Bindings set(Map<String, Set<Bond>> nodes) {
        try
        {
            Bindings newBindings = new Bindings(nodes);
            String json = mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(newBindings);
            FileUtils.writeStringToFile(new File(STORAGE), json);
            log.info("Wrote {} bonds to file '{}'", newBindings.bonds().size(), STORAGE);
            bindings = newBindings;

        } catch (Exception e) {
            log.error(
                    "Cannot write bindings info to file '{}' - '{}', changes are not saved",
                    STORAGE, e.getMessage());
            log.debug("Exception trace", e);
        }

        return bindings;
    }

    public Bond bind(String catalogId, Bond bond)
    {
        Map<String, Set<Bond>> newBindings = new HashMap<>(bindings.bonds());
        Set<Bond> bonds;
        if (newBindings.get(catalogId) == null)
        {
            bonds = new HashSet<>();
            newBindings.put(catalogId, bonds);
        } else
            bonds = newBindings.get(catalogId);

        if (bonds.contains(bond)) {
            //FIXME: lol, replace
            bonds.remove(bond);
            bonds.add(bond);
        } else {
            bonds.add(bond);
        }
        set(newBindings);
        return bond;
    }

    public Bond stage(String catalogId, String merlionId)
    {
        Bond bond = new Bond(merlionId, catalogId, "-1");
        return bind(catalogId, bond);
    }

    public List<Bond> staging() {
        List<Bond> bonds = new ArrayList<>();
        get().bonds().values().forEach(
                bs -> bs.stream().filter(b -> b.id().equals("-1")).forEach(bonds::add)
        );
        return bonds;
    }

    public Bindings unbindByMerlId(String merlionId)
    {
        Map<String, Set<Bond>> bonds = new HashMap<>();
        for(Map.Entry<String, Set<Bond>> entry: bindings.bonds().entrySet())
            bonds.put(entry.getKey(),
                    entry.getValue()
                        .stream()
                        .filter(bond -> !bond.merlionId().equals(merlionId))
                        .collect(Collectors.toSet()));

        return set(bonds);
    }

    public Bindings unbindById(String id)
    {
        Map<String, Set<Bond>> bonds = new HashMap<>();
        for(Map.Entry<String, Set<Bond>> entry: bindings.bonds().entrySet())
            bonds.put(entry.getKey(),
                    entry.getValue()
                            .stream()
                            .filter(bond ->
                                            !bond.id().equals(id)
                            )
                            .collect(Collectors.toSet()));

        return set(bonds);
    }

    public Bindings unbind(String merlionId, String id)
    {
        Map<String, Set<Bond>> bonds = new HashMap<>();
        for(Map.Entry<String, Set<Bond>> entry: bindings.bonds().entrySet())
            bonds.put(entry.getKey(),
                    entry.getValue()
                            .stream()
                            .filter(
                                bond ->
                                        !bond.merlionId().equals(merlionId) &&
                                        !bond.id().equals(id)
                            )
                            .collect(Collectors.toSet()));

        return set(bonds);
    }

    public Bond searchByMerlionId(String merlionId)
    {
        Bond ret = null;
        for(Map.Entry<String, Set<Bond>> entry: bindings.bonds().entrySet())
        {
            ret = entry.getValue()
                    .stream()
                    .filter(
                            bond -> bond.merlionId().equals(merlionId)
                    )
                    .findAny()
                    .orElse(null);
            if(ret != null)
                break;
        }
        return ret;
    }

    public Bond searchById(String id)
    {
        Bond ret = null;
        for(Map.Entry<String, Set<Bond>> entry: bindings.bonds().entrySet())
        {
            ret = entry.getValue()
                    .stream()
                    .filter(
                            bond -> bond.id().equals(id)
                    )
                    .findAny()
                    .orElse(null);
            if(ret != null)
                break;
        }
        return ret;
    }

    public Bond search(String merlionId, String id)
    {
        Bond ret = null;
        for(Map.Entry<String, Set<Bond>> entry: bindings.bonds().entrySet())
        {
            ret = entry.getValue()
                    .stream()
                    .filter(
                            bond -> bond.id().equals(id) &&
                                    bond.merlionId().equals(merlionId)
                    )
                    .findAny()
                    .orElse(null);
            if(ret != null)
                break;
        }
        return ret;
    }
}
