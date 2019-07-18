package com.snavi.swiftlift.searching;

/**
 * CellCreator contains functions to assign "Cell" (cell description in a moment) for given
 * coordinates. Also you can find here function getSearchedCells(long cell, int range) that returns
 * Cells around given Cell.
 *
 * What is "Cell"?
 * Cell is number of type long (actually 32 bits - one bit too much to fit it into int :-( ) that
 * represents square on the Earth surface. the square side of about 5.5 km. Binary representation
 * of Cell: | constant 1 - to prevent it from shrinking | 1 - latitude on north hemisphere, 0 -
 * latitude on south hemisphere | 14 bits for latitude | 1 - longitude on east hemisphere, 0 -
 * longitude on west hemisphere | 15 bits for longitude |
 *
 * Why "Cell" was introduced?
 * The app stands on Firebase and at the moment when I am writing it Firebase doesn't allow to
 * compare > or < for more than one field in a single query. For this reason I couldn't search for
 * exact limit coordinates. Also it doesn't allow to doing operations on fields in database in
 * query, so I had to figure out something else. And that "something else" happen to be the
 * Cell. Each lift/ride will have Cell assigned, so that it can be easily and efficiently found
 * when user is looking for lifts in particular area. This solution is even better, because
 * Cell is just a long number, so it can be indexed and search will be very fast event for huge
 * amount of lifts/rides.
 */
public class CellCreator {

    // CONST //////////////////////////////////////////////////////////////////////////////////////
    private static final long CELL_BASE             = 2147483648L;  // | 1 | 31 x 0 bit |
    private static final long LAT_HEMISPHERE_MASK   = 1073741824L;  // | 0 | 1/0 | 30 x 0 bit |
    private static final int  LON_HEMISPHERE_MASK   = 32768;        // | 16 x 0 bit | 1/0 | 15 x 0 bit |
    private static final int  LAT_DIV               = 65536;        // 2 ^ 16
    private static final int  LON_PART_MASK         = 65535;        // 16 last bits = lon hemisphere + lon
    private static final long NON_LON_PART_MASK     = 4294901760L;  // 16 first bits = const 1 + lat part
    private static final long LAT_PART_MASK         = 2147418112L;  // | 0 | 15 x 1 bit | 16 x 0 bit |

    private static final int N_DIRECTION = 0;
    private static final int E_DIRECTION = 1;
    private static final int S_DIRECTION = 2;
    private static final int W_DIRECTION = 3;

    private static final int JUMP        = 5;               // how much 0.01 of coordinate per cell

    private static final int  MAX_E_CELL = 50763;           // east most cell (with no latitude part)
    private static final int  MAX_W_CELL = 18000;           // west most cell (with no latitude part)
    private static final int  MIN_E_CELL = 32768;           // the closest cell to 0 meridian on east side (with no latitude)
    private static final int  MIN_W_CELL = 5;               // the closest cell to 0 meridian on west side (with no latitude)
    private static final long MIN_N_CELL = 1073741824L;     // the closest cell to equator on northern hemisphere (zeros for longitude)
    private static final int  MIN_S_CELL = 327680;          // the closest cell to equator on southern hemisphere (zeros fon longitude)



    /**
     * assigns cell for given latitude and longitude
     * @param lat latitude for which cell will be assigned
     * @param lon longitude for which cell will be assigned
     * @return cell (long representing some square (side = JUMP) on the earth surface
     */
    public static long assignCell(double lat, double lon)
    {
        return
                CELL_BASE +
                        processCoordinate(lat, LAT_HEMISPHERE_MASK, LAT_DIV) +
                        processCoordinate(lon, LON_HEMISPHERE_MASK, 1);
    }


    /**
     * function that takes single coordinate (latitude/longitude) and returns it in format that is
     * easily applicable to cell. In binary it looks like that:
     * | hemisphere bit | 15 coordinate bits | log(2)(mult) zero bits |
     * @param coordinate coordinate to process
     * @param hemisphereBit if you are processing latitude coordinate - 1 for coordinates on
     *                      northern hemisphere and 0 for coordinates on southern hemisphere.
     *                      If you are processing longitude coordinate - 1 for eastern hemisphere
     *                      and 0 for western hemisphere
     * @param mult 2^(how much 0 bits to add after processed coordinate). For longitude it should
     *             be 1 (2^0) and for latitude it should be 65536 (2^16).
     * @return processed coordinate that can be simply added to cell for which this kind of
     * coordinate hasn't been added yet. For example if you have empty cell, that is CELL_BASE,
     * you can simply add processed latitude and longitude to it, order doesn't matter
     */
    private static long processCoordinate(double coordinate, long hemisphereBit, long mult)
    {
        long res = 0;

        // for east hemisphere
        if (coordinate >= 0)
        {
            res += hemisphereBit;

            int processed = (int) (coordinate * 100);
            if (processed % 10 >= 5)
                processed = processed / 10 * 10 + JUMP;
            else
                processed = processed / 10 * 10;

            return res + (processed * mult);
        }
        else
        {
            coordinate = Math.abs(coordinate);
            int processed = (int) (coordinate * 100);
            if (processed % 10 <= 5)
                processed = processed / 10 * 10 + JUMP;
            else
                processed = processed / 10 * 10 + (2 * JUMP);

            return res + (processed * mult);
        }
    }



    /**
     * function that finds direct neighbour of cell to the East (direction = E_DIRECTION) or
     * West (direction = W_DIRECTION)
     * @param cell cell for which neighbour will be found
     * @param direction direction in which searched neighbour is (E_DIRECTION, W_DIRECTION)
     * @return found neighbour
     */
    private static long getLonNeighbour(long cell, int direction)
    {
        int lonPart = (int) (cell & LON_PART_MASK);
        long nonLonPart = cell & NON_LON_PART_MASK;

        // extremes
        if (lonPart == MAX_E_CELL && direction == E_DIRECTION)      // the east most cell
            return nonLonPart + MAX_W_CELL;
        if (lonPart == MAX_W_CELL && direction == W_DIRECTION)      // the west most cell
            return nonLonPart + MAX_E_CELL;
        if (lonPart == MIN_E_CELL && direction == W_DIRECTION)      // the closest cell to the 0 meridian on east side
            return nonLonPart + MIN_W_CELL;
        if (lonPart == MIN_W_CELL && direction == E_DIRECTION)      // the closest cell to the 0 meridian on west side
            return nonLonPart + MIN_E_CELL;

        // typical case
        boolean isEast = (cell & LON_HEMISPHERE_MASK) != 0;

        // East
        if (isEast)
        {
            if (direction == E_DIRECTION)
                return nonLonPart + lonPart + JUMP;
            else
                return nonLonPart + lonPart - JUMP;
        }
        else
        {
            if (direction == E_DIRECTION)
                return nonLonPart + lonPart - JUMP;
            else
                return nonLonPart + lonPart + JUMP;
        }
    }


    /**
     * function that finds direct neighbour of cell to the North (direction = N_DIRECTION) or
     * South (direction = S_DIRECTION)
     * @param cell cell for which neighbour will be found
     * @param direction direction in which searched neighbour is (N_DIRECTION, S_DIRECTION)
     * @return found neighbour
     */
    private static long getLatNeighbour(long cell, int direction)
    {
        long latPart = (cell & LAT_PART_MASK);                              // 01111111111111110000000000000000
        long lonPart = (cell & LON_PART_MASK);                              // 00000000000000001111111111111111

        // extremes
        if (latPart == MIN_N_CELL && direction == S_DIRECTION)              // the closest cell to equator on northern hemisphere
            return CELL_BASE + MIN_S_CELL + lonPart;
        if (latPart == MIN_S_CELL && direction == N_DIRECTION)              // the closest cell to equator on southern hemisphere
            return CELL_BASE + MIN_N_CELL + lonPart;

        // fuck North/South pole, no one is going to take a ride there :)

        // typical case
        boolean isNorth = (cell & LAT_HEMISPHERE_MASK) != 0;
        long    jump    = JUMP * LAT_DIV;                                   // how much to add/subtract to get neighbour

        if (isNorth)
        {
            if (direction == N_DIRECTION)
                return CELL_BASE + latPart + jump + lonPart;
            else
                return CELL_BASE + latPart - jump + lonPart;
        }
        else
        {
            if (direction == N_DIRECTION)
                return CELL_BASE + latPart - jump + lonPart;
            else
                return CELL_BASE + latPart + jump + lonPart;
        }
    }



    /**
     * function, that returns linear longitude neighbours for cell
     * @param cell cell for which neighbours will be found
     * @param range how much neighbours in each direction (East/West)
     * @return long[2 x range] array of found neighbours
     */
    private static long[] getLonNeighbours(long cell, int range)
    {
        int    resSize  = range * 2;
        long[] res      = new long[resSize];
        long   currCell = cell;

        // west
        for (int i = 0; i < range; i++)
        {
            res[i] = getLonNeighbour(currCell, W_DIRECTION);
            currCell = res[i];
        }

        currCell = cell;

        // east
        for (int i = range; i < resSize; i++)
        {
            res[i] = getLonNeighbour(currCell, E_DIRECTION);
            currCell = res[i];
        }

        return res;
    }


    /**
     * function, that returns linear latitude neighbours for cell
     * @param cell cell for which neighbours will be found
     * @param range how much neighbours in each direction (North/South)
     * @return long[2 x range] array of found neighbours
     */
    private static long[] getLatNeighbours(long cell, int range)
    {
        int    resSize  = range * 2;
        long[] res      = new long[resSize];
        long   currCell = cell;

        // south
        for (int i = 0; i < range; i++)
        {
            res[i] = getLatNeighbour(currCell, S_DIRECTION);
            currCell = res[i];
        }

        currCell = cell;

        // north
        for (int i = range; i < resSize; i++)
        {
            res[i] = getLatNeighbour(currCell, N_DIRECTION);
            currCell = res[i];
        }

        return res;
    }


    /**
     * function, that finds square (2 * range + 1) x (2 * range + 1) of cells.
     * example for cell = x and range = 2
     *  _ _ _ _ _
     * |_|_|_|_|_|
     * |_|_|_|_|_|
     * |_|_|x|_|_|
     * |_|_|_|_|_|
     * |_|_|_|_|_|
     *
     * @param cell cell in which user chosen location is
     * @param range how much neighbours in each side to find
     * @return long[] array of searched cells
     */
    public static long[] getSearchedCells(long cell, int range)
    {
        int    size = (2 * range + 1) * (2 * range + 1);                                // just math
        long[] res  = new long[size];                                                   // array of searched cells

        // longitude neighbours
        int    singleNeighSize = range * 2;                                             // num of linear neighbours for single cell
        long[] lonNeighbours   = getLonNeighbours(cell, range);                         // longitude neighbours for init cell
        System.arraycopy(lonNeighbours, 0, res, 0, lonNeighbours.length);         // add neighbours to result

        // latitude neighbours
        int    posInRes             = lonNeighbours.length;                             // position at which new neighbours should be appended to res
        long[] singleLatNeighbours;

        for (long lonNeighbour : lonNeighbours)                                         // find latitude neighbours for each longitude neighbour
        {
            singleLatNeighbours = getLatNeighbours(lonNeighbour, range);
            System.arraycopy(singleLatNeighbours, 0, res, posInRes,
                    singleNeighSize);
            posInRes += singleNeighSize;
        }

        long[] zeroLineLatNeighbours = getLatNeighbours(cell, range);                   // append neighbours of init cell
        System.arraycopy(zeroLineLatNeighbours, 0, res, posInRes,
                zeroLineLatNeighbours.length);

        res[size - 1] = cell;                                                           // append init cell itself

        return res;
    }
}
