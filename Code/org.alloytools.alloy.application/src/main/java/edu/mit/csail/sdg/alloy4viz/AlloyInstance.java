/* Alloy Analyzer 4 -- Copyright (c) 2006-2009, Felix Chang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package edu.mit.csail.sdg.alloy4viz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.translator.A4Solution;

/**
 * Immutable; represents an Alloy instance that can be displayed in the
 * visualizer.
 * <p>
 * <b>Thread Safety:</b> Can be called only by the AWT event thread
 */

public final class AlloyInstance {

    /** The original A4Solution object. */
    public final A4Solution originalA4;					// FIXTHIS:
    // eventually
    // we
    // shouldn't
    // need
    // this
    // field...

    /**
     * If true, it is a metamodel, else it is not a metamodel.
     */
    public final boolean                             isMetamodel;

    /**
     * The original filename of the model that generated this instance; can be "" if
     * unknown.
     */
    public final String                              filename;

    /**
     * The original command that generated this instance; can be "" if unknown.
     */
    public final String                              commandname;

    /**
     * The AlloyModel that this AlloyInstance is an instance of.
     */
    public final AlloyModel                          model;

    /**
     * Maps each AlloyAtom to the AlloySet(s) it is in; its keySet is considered the
     * universe of all atoms. <br>
     * The constructor ensures every AlloySet here is in model.getSets() <br>
     * Furthermore, every AlloyAtom's type is in model.getTypes() <br>
     * Finally, if an atom A is in a set S, we guarantee that A.type is equal or
     * subtype of S.type
     */
    private final Map<AlloyAtom,ConstList<AlloySet>> atom2sets;

    /**
     * Maps each AlloyType to the AlloyAtom(s) in that type; it is derived from
     * atom2sets.keySet() directly. <br>
     * Thus, every AlloyType here is in model.getTypes(), and every AlloyAtom here
     * is in atom2sets.keySet() <br>
     * Furthermore, the constructor ensures that if an atom is in a subtype, it is
     * also in the supertype.
     */
    private final Map<AlloyType,List<AlloyAtom>>     type2atoms;

    /**
     * Maps each AlloySet to the AlloyAtom(s) in that set; it is derived from
     * atom2sets directly. <br>
     * Thus, every AlloySet here is in model.getSets(), and every AlloyAtom here is
     * in atom2sets.keySet() <br>
     * Finally, if an atom A is in a set S, we guarantee that A.type is equal or
     * subtype of S.type
     */
    private final Map<AlloySet,List<AlloyAtom>>      set2atoms;

    /**
     * Maps each AlloyRelation to a set of AlloyTuple(s). <br>
     * The constructor ensures every AlloyRelation here is in model.getRelations()
     * <br>
     * Furthermore, every AlloyAtom in every AlloyTuple here is in
     * atom2sets.keySet() <br>
     * Finally, if a tuple T is in a relation R, we guarantee that T is a legal
     * tuple for R (Meaning T.arity==R.arity, and for each i, T.getAtom(i).type is
     * equal or subtype of R.getType(i).)
     */
    private final Map<AlloyRelation,Set<AlloyTuple>> rel2tuples;

    // ====== UNCERTAIN TUPLE SUPPORT FIELDS ======

    /** True iff this AlloyInstance contains uncertain tuple data. */
    private final boolean                            hasUncertainTuples;

    /**
     * Maps each AlloyAtom to uncertain AlloySet(s) it might be in; parallel to
     * atom2sets. Only populated if hasUncertainTuples is true.
     */
    private final Map<AlloyAtom,ConstList<AlloySet>> uncertainAtom2sets;

    /**
     * Maps each AlloyType to uncertain AlloyAtom(s) that might be in that type;
     * parallel to type2atoms. Only populated if hasUncertainTuples is true.
     */
    private final Map<AlloyType,List<AlloyAtom>>     uncertainType2atoms;

    /**
     * Maps each AlloySet to uncertain AlloyAtom(s) that might be in that set;
     * parallel to set2atoms. Only populated if hasUncertainTuples is true.
     */
    private final Map<AlloySet,List<AlloyAtom>>      uncertainSet2atoms;

    /**
     * Maps each AlloyRelation to uncertain AlloyTuple(s) that might be in that
     * relation; parallel to rel2tuples. Only populated if hasUncertainTuples is
     * true.
     */
    private final Map<AlloyRelation,Set<AlloyTuple>> uncertainRel2tuples;

    /**
     * This always stores an empty unmodifiable list of atoms.
     */
    private static final List<AlloyAtom>             noAtom           = ConstList.make();

    /**
     * This always stores an empty unmodifiable list of sets.
     */
    private static final List<AlloySet>              noSet            = ConstList.make();

    /**
     * This always stores an empty unmodifiable set of tuples.
     */
    private static final Set<AlloyTuple>             noTuple          = Collections.unmodifiableSet(new TreeSet<AlloyTuple>());

    /**
     * This always stores an empty unmodifiable list of uncertain atoms.
     */
    private static final List<AlloyAtom>             noUncertainAtom  = ConstList.make();

    /**
     * This always stores an empty unmodifiable list of uncertain sets.
     */
    private static final List<AlloySet>              noUncertainSet   = ConstList.make();

    /**
     * This always stores an empty unmodifiable set of uncertain tuples.
     */
    private static final Set<AlloyTuple>             noUncertainTuple = Collections.unmodifiableSet(new TreeSet<AlloyTuple>());

    /**
     * Create a new instance.
     *
     * @param filename - the original filename of the model that generated this
     *            instance; can be "" if unknown
     * @param commandname - the original command that generated this instance; can
     *            be "" if unknown
     * @param model - the AlloyModel that this AlloyInstance is an instance of
     * @param atom2sets - maps each atom to the set(s) it is in; its KeySet is
     *            considered the universe of all atoms
     * @param rel2tuples - maps each relation to the tuple(s) it is in
     *            <p>
     *            (The constructor will ignore any atoms, sets, types, and relations
     *            not in the model. So there's no need to explicitly filter them out
     *            prior to passing "atom2sets" and "rel2tuples" to the constructor.)
     */
    public AlloyInstance(A4Solution originalA4, String filename, String commandname, AlloyModel model, Map<AlloyAtom,Set<AlloySet>> atom2sets, Map<AlloyRelation,Set<AlloyTuple>> rel2tuples, boolean isMetamodel) {
        this.originalA4 = originalA4;
        this.filename = filename;
        this.commandname = commandname;
        this.model = model;
        this.isMetamodel = isMetamodel;
        // First, construct atom2sets (Use a treemap because we want its keyset
        // to be sorted)
        {
            Map<AlloyAtom,ConstList<AlloySet>> a2s = new TreeMap<AlloyAtom,ConstList<AlloySet>>();
            for (Map.Entry<AlloyAtom,Set<AlloySet>> e : atom2sets.entrySet()) {
                AlloyAtom atom = e.getKey();
                if (!model.hasType(atom.getType()))
                    continue; // We discard any AlloyAtom whose type is not in
                             // this model
                             // We discard any AlloySet not in this model; and we discard
                             // AlloySet(s) that don't match the atom's type
                List<AlloySet> sets = new ArrayList<AlloySet>();
                for (AlloySet set : e.getValue())
                    if (model.getSets().contains(set) && model.isEqualOrSubtype(atom.getType(), set.getType()))
                        sets.add(set);
                Collections.sort(sets);
                a2s.put(atom, ConstList.make(sets));
            }
            this.atom2sets = Collections.unmodifiableMap(a2s);
        }
        // Next, construct set2atoms
        {
            Map<AlloySet,List<AlloyAtom>> s2a = new LinkedHashMap<AlloySet,List<AlloyAtom>>();
            for (Map.Entry<AlloyAtom,ConstList<AlloySet>> e : this.atom2sets.entrySet())
                for (AlloySet set : e.getValue()) {
                    List<AlloyAtom> atoms = s2a.get(set);
                    if (atoms == null) {
                        atoms = new ArrayList<AlloyAtom>();
                        s2a.put(set, atoms);
                    }
                    atoms.add(e.getKey());
                }
            for (AlloySet set : model.getSets()) {
                List<AlloyAtom> atoms = s2a.get(set);
                if (atoms == null)
                    continue;
                Collections.sort(atoms);
                s2a.put(set, Collections.unmodifiableList(atoms));
            }
            this.set2atoms = Collections.unmodifiableMap(s2a);
        }
        // Next, construct type2atoms
        {
            Map<AlloyType,List<AlloyAtom>> t2a = new LinkedHashMap<AlloyType,List<AlloyAtom>>();
            for (AlloyAtom a : this.atom2sets.keySet()) {
                for (AlloyType t = a.getType(); t != null; t = model.getSuperType(t)) {
                    List<AlloyAtom> atoms = t2a.get(t);
                    if (atoms == null) {
                        atoms = new ArrayList<AlloyAtom>();
                        t2a.put(t, atoms);
                    }
                    atoms.add(a);
                }
            }
            for (AlloyType t : model.getTypes()) {
                List<AlloyAtom> atoms = t2a.get(t);
                if (atoms == null)
                    continue;
                Collections.sort(atoms);
                t2a.put(t, Collections.unmodifiableList(atoms));
            }
            this.type2atoms = Collections.unmodifiableMap(t2a);
        }
        // Finally, construct rel2tuples
        Map<AlloyRelation,Set<AlloyTuple>> r2t = new LinkedHashMap<AlloyRelation,Set<AlloyTuple>>();
        for (Map.Entry<AlloyRelation,Set<AlloyTuple>> e : rel2tuples.entrySet()) {
            AlloyRelation rel = e.getKey();
            if (!model.getRelations().contains(rel))
                continue; // We discard any AlloyRelation not in this model
            Set<AlloyTuple> tuples = new TreeSet<AlloyTuple>();
            for (AlloyTuple tuple : e.getValue()) {
                if (tuple.getArity() != rel.getArity())
                    continue; // The arity must match
                for (int i = 0;; i++) {
                    if (i == tuple.getArity()) {
                        tuples.add(tuple);
                        break;
                    }
                    AlloyAtom a = tuple.getAtoms().get(i);
                    if (!this.atom2sets.containsKey(a))
                        break; // Every atom must exist
                    if (!model.isEqualOrSubtype(a.getType(), rel.getTypes().get(i)))
                        break; // Atom must match the type
                }
            }
            if (tuples.size() != 0)
                r2t.put(rel, Collections.unmodifiableSet(tuples));
        }
        this.rel2tuples = Collections.unmodifiableMap(r2t);

        // Initialize uncertain tuple support
        this.hasUncertainTuples = originalA4.hasUncertainTuples();

        if (hasUncertainTuples) {
            // Extract uncertain tuple data from A4Solution
            this.uncertainAtom2sets = extractUncertainAtom2Sets(originalA4, model);
            this.uncertainType2atoms = buildUncertainType2Atoms();
            this.uncertainSet2atoms = buildUncertainSet2Atoms();
            this.uncertainRel2tuples = extractUncertainRel2Tuples(originalA4, model);
        } else {
            // Initialize as empty maps when no uncertain data
            this.uncertainAtom2sets = Collections.emptyMap();
            this.uncertainType2atoms = Collections.emptyMap();
            this.uncertainSet2atoms = Collections.emptyMap();
            this.uncertainRel2tuples = Collections.emptyMap();
        }
    }

    /**
     * Returns an unmodifiable sorted set of all AlloyAtoms in this AlloyInstance.
     */
    public Set<AlloyAtom> getAllAtoms() {
        return Collections.unmodifiableSet(atom2sets.keySet());
    }

    /**
     * Returns an unmodifiable sorted list of AlloySet(s) that this atom is in;
     * answer can be an empty list.
     */
    public List<AlloySet> atom2sets(AlloyAtom atom) {
        List<AlloySet> answer = atom2sets.get(atom);
        return answer != null ? answer : noSet;
    }

    /**
     * Returns an unmodifiable sorted list of AlloyAtom(s) in this type; answer can
     * be an empty list.
     */
    public List<AlloyAtom> type2atoms(AlloyType type) {
        List<AlloyAtom> answer = type2atoms.get(type);
        return answer != null ? answer : noAtom;
    }

    /**
     * Returns an unmodifiable sorted list of AlloyAtom(s) in this set; answer can
     * be an empty list.
     */
    public List<AlloyAtom> set2atoms(AlloySet set) {
        List<AlloyAtom> answer = set2atoms.get(set);
        return answer != null ? answer : noAtom;
    }

    /**
     * Returns an unmodifiable sorted set of AlloyTuple(s) in this relation; answer
     * can be an empty set.
     */
    public Set<AlloyTuple> relation2tuples(AlloyRelation rel) {
        Set<AlloyTuple> answer = rel2tuples.get(rel);
        return answer != null ? answer : noTuple;
    }

    // ====== UNCERTAIN TUPLE ACCESS METHODS ======

    /**
     * Returns true if this AlloyInstance contains uncertain tuple data.
     */
    public boolean hasUncertainTuples() {
        return hasUncertainTuples;
    }

    /**
     * Returns an unmodifiable sorted list of uncertain AlloySet(s) that this atom
     * might be in; answer can be an empty list. This is separate from the definite
     * atom2sets() method.
     */
    public List<AlloySet> uncertainAtom2sets(AlloyAtom atom) {
        if (!hasUncertainTuples)
            return noUncertainSet;
        ConstList<AlloySet> answer = uncertainAtom2sets.get(atom);
        return answer != null ? answer : noUncertainSet;
    }

    /**
     * Returns an unmodifiable sorted list of uncertain AlloyAtom(s) that might be
     * in this type; answer can be an empty list. This is separate from the definite
     * type2atoms() method.
     */
    public List<AlloyAtom> uncertainType2atoms(AlloyType type) {
        if (!hasUncertainTuples)
            return noUncertainAtom;
        List<AlloyAtom> answer = uncertainType2atoms.get(type);
        return answer != null ? answer : noUncertainAtom;
    }

    /**
     * Returns an unmodifiable sorted list of uncertain AlloyAtom(s) that might be
     * in this set; answer can be an empty list. This is separate from the definite
     * set2atoms() method.
     */
    public List<AlloyAtom> uncertainSet2atoms(AlloySet set) {
        if (!hasUncertainTuples)
            return noUncertainAtom;
        List<AlloyAtom> answer = uncertainSet2atoms.get(set);
        return answer != null ? answer : noUncertainAtom;
    }

    /**
     * Returns an unmodifiable sorted set of uncertain AlloyTuple(s) that might be
     * in this relation; answer can be an empty set. This is separate from the
     * definite relation2tuples() method.
     */
    public Set<AlloyTuple> uncertainRelation2tuples(AlloyRelation rel) {
        if (!hasUncertainTuples)
            return noUncertainTuple;
        Set<AlloyTuple> answer = uncertainRel2tuples.get(rel);
        return answer != null ? answer : noUncertainTuple;
    }

    /**
     * Two instances are equal if they have the same filename, same commands, same
     * model, and same atoms and tuples relationships, including uncertain data.
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AlloyInstance))
            return false;
        if (other == this)
            return true;
        AlloyInstance x = (AlloyInstance) other;
        if (!filename.equals(x.filename))
            return false;
        if (!commandname.equals(x.commandname))
            return false;
        if (!model.equals(x.model))
            return false;
        if (!atom2sets.equals(x.atom2sets))
            return false;
        if (!type2atoms.equals(x.type2atoms))
            return false;
        if (!set2atoms.equals(x.set2atoms))
            return false;
        if (!rel2tuples.equals(x.rel2tuples))
            return false;
        // Include uncertain tuple data in equality check
        if (hasUncertainTuples != x.hasUncertainTuples)
            return false;
        if (!uncertainAtom2sets.equals(x.uncertainAtom2sets))
            return false;
        if (!uncertainType2atoms.equals(x.uncertainType2atoms))
            return false;
        if (!uncertainSet2atoms.equals(x.uncertainSet2atoms))
            return false;
        if (!uncertainRel2tuples.equals(x.uncertainRel2tuples))
            return false;
        return true;
    }

    /**
     * Computes a hash code based on the same information used in equals(),
     * including uncertain data.
     */
    @Override
    public int hashCode() {
        int n = 5 * filename.hashCode() + 7 * commandname.hashCode();
        n = n + 7 * atom2sets.hashCode() + 31 * type2atoms.hashCode() + 71 * set2atoms.hashCode() + 3 * rel2tuples.hashCode();
        // Include uncertain tuple data in hash code
        n = n + (hasUncertainTuples ? 1 : 0);
        n = n + 11 * uncertainAtom2sets.hashCode() + 13 * uncertainType2atoms.hashCode();
        n = n + 17 * uncertainSet2atoms.hashCode() + 19 * uncertainRel2tuples.hashCode();
        return 17 * n + model.hashCode();
    }

    /**
     * Returns a textual dump of the instance, including uncertain data if present.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Instance's model:\n");
        sb.append(model.toString());
        sb.append("Instance's atom2sets:\n");
        for (Map.Entry<AlloyAtom,ConstList<AlloySet>> entry : atom2sets.entrySet()) {
            sb.append("  ");
            sb.append(entry.getKey());
            sb.append(" ");
            sb.append(entry.getValue());
            sb.append('\n');
        }
        sb.append("Instance's rel2tuples:\n");
        for (Map.Entry<AlloyRelation,Set<AlloyTuple>> entry : rel2tuples.entrySet()) {
            sb.append("  ");
            sb.append(entry.getKey());
            sb.append(" ");
            sb.append(entry.getValue());
            sb.append('\n');
        }
        // Include uncertain tuple information if present
        if (hasUncertainTuples) {
            sb.append("Instance's uncertain atom2sets:\n");
            for (Map.Entry<AlloyAtom,ConstList<AlloySet>> entry : uncertainAtom2sets.entrySet()) {
                sb.append("  uncertain: ");
                sb.append(entry.getKey());
                sb.append(" ");
                sb.append(entry.getValue());
                sb.append('\n');
            }
            sb.append("Instance's uncertain rel2tuples:\n");
            for (Map.Entry<AlloyRelation,Set<AlloyTuple>> entry : uncertainRel2tuples.entrySet()) {
                sb.append("  uncertain: ");
                sb.append(entry.getKey());
                sb.append(" ");
                sb.append(entry.getValue());
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    // ====== UNCERTAIN TUPLE EXTRACTION HELPER METHODS ======

    /**
     * Extracts uncertain atom-to-sets mappings from A4Solution. This method
     * converts A4Solution uncertain tuples to AlloyInstance format.
     *
     * Note: This is a placeholder implementation that returns empty data. The full
     * implementation would extract uncertain tuple data from the A4Solution and
     * convert it to the AlloyInstance format, but requires access to Kodkod types.
     */
    private Map<AlloyAtom,ConstList<AlloySet>> extractUncertainAtom2Sets(A4Solution sol, AlloyModel model) {
        // Placeholder implementation - returns empty map
        // The full implementation would:
        // 1. Iterate through model.getRelations()
        // 2. For each relation, find corresponding Kodkod relation in sol
        // 3. Call sol.getUncertainTuples(kodkodRelation)
        // 4. Convert Kodkod tuples to AlloyAtom-AlloySet associations
        // 5. Build the final mapping similar to the existing atom2sets construction

        return Collections.emptyMap();
    }

    /**
     * Extracts uncertain relation-to-tuples mappings from A4Solution.
     *
     * Note: This is a placeholder implementation that returns empty data.
     */
    private Map<AlloyRelation,Set<AlloyTuple>> extractUncertainRel2Tuples(A4Solution sol, AlloyModel model) {
        // Placeholder implementation - returns empty map
        // The full implementation would:
        // 1. Iterate through model.getRelations()
        // 2. For each relation, find corresponding Kodkod relation in sol
        // 3. Call sol.getUncertainTuples(kodkodRelation)
        // 4. Convert Kodkod tuples to AlloyTuples
        // 5. Validate tuples and build the final mapping

        return Collections.emptyMap();
    }

    /**
     * Builds uncertain type-to-atoms mapping from uncertain atom-to-sets data. Uses
     * same logic as existing type2atoms construction.
     */
    private Map<AlloyType,List<AlloyAtom>> buildUncertainType2Atoms() {
        Map<AlloyType,List<AlloyAtom>> result = new LinkedHashMap<AlloyType,List<AlloyAtom>>();

        // Build mapping using same logic as existing type2atoms
        for (AlloyAtom atom : uncertainAtom2sets.keySet()) {
            // Add atom to all its supertypes (same as existing logic)
            for (AlloyType type = atom.getType(); type != null; type = model.getSuperType(type)) {
                List<AlloyAtom> atoms = result.get(type);
                if (atoms == null) {
                    atoms = new ArrayList<AlloyAtom>();
                    result.put(type, atoms);
                }
                atoms.add(atom);
            }
        }

        // Sort and make immutable (same as existing logic)
        for (Map.Entry<AlloyType,List<AlloyAtom>> entry : result.entrySet()) {
            Collections.sort(entry.getValue());
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Builds uncertain set-to-atoms mapping from uncertain atom-to-sets data. Uses
     * same logic as existing set2atoms construction.
     */
    private Map<AlloySet,List<AlloyAtom>> buildUncertainSet2Atoms() {
        Map<AlloySet,List<AlloyAtom>> result = new LinkedHashMap<AlloySet,List<AlloyAtom>>();

        // Build mapping using same logic as existing set2atoms
        for (Map.Entry<AlloyAtom,ConstList<AlloySet>> entry : uncertainAtom2sets.entrySet()) {
            AlloyAtom atom = entry.getKey();
            for (AlloySet set : entry.getValue()) {
                List<AlloyAtom> atoms = result.get(set);
                if (atoms == null) {
                    atoms = new ArrayList<AlloyAtom>();
                    result.put(set, atoms);
                }
                atoms.add(atom);
            }
        }

        // Sort and make immutable (same as existing logic)
        for (Map.Entry<AlloySet,List<AlloyAtom>> entry : result.entrySet()) {
            Collections.sort(entry.getValue());
            entry.setValue(Collections.unmodifiableList(entry.getValue()));
        }

        return Collections.unmodifiableMap(result);
    }
}
