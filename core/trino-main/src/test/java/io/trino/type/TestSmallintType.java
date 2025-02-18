/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.type;

import io.trino.spi.block.Block;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.type.Type.Range;

import static io.trino.spi.type.SmallintType.SMALLINT;
import static org.testng.Assert.assertEquals;

public class TestSmallintType
        extends AbstractTestType
{
    public TestSmallintType()
    {
        super(SMALLINT, Short.class, createTestBlock());
    }

    public static Block createTestBlock()
    {
        BlockBuilder blockBuilder = SMALLINT.createBlockBuilder(null, 15);
        SMALLINT.writeLong(blockBuilder, 1111);
        SMALLINT.writeLong(blockBuilder, 1111);
        SMALLINT.writeLong(blockBuilder, 1111);
        SMALLINT.writeLong(blockBuilder, 2222);
        SMALLINT.writeLong(blockBuilder, 2222);
        SMALLINT.writeLong(blockBuilder, 2222);
        SMALLINT.writeLong(blockBuilder, 2222);
        SMALLINT.writeLong(blockBuilder, 2222);
        SMALLINT.writeLong(blockBuilder, 3333);
        SMALLINT.writeLong(blockBuilder, 3333);
        SMALLINT.writeLong(blockBuilder, 4444);
        return blockBuilder.build();
    }

    @Override
    protected Object getGreaterValue(Object value)
    {
        return ((Long) value) + 1;
    }

    @Override
    public void testRange()
    {
        Range range = type.getRange().orElseThrow();
        assertEquals(range.getMin(), (long) Short.MIN_VALUE);
        assertEquals(range.getMax(), (long) Short.MAX_VALUE);
    }
}
