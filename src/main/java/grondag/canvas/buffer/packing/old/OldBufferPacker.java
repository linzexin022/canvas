/*******************************************************************************
 * Copyright 2019 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.buffer.packing.old;

import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import grondag.canvas.buffer.allocation.AllocationProvider;
import grondag.canvas.buffer.encoding.old.OldMaterialVertexFormat;
import grondag.canvas.chunk.draw.DelegateLists;
import grondag.canvas.chunk.draw.DrawableDelegate;
import grondag.canvas.material.old.OldMaterialState;

public class OldBufferPacker {
	private static final ThreadLocal<OldBufferPacker> THREADLOCAL = ThreadLocal.withInitial(OldBufferPacker::new);

	ObjectArrayList<DrawableDelegate> delegates;
	OldVertexCollectorList collectorList;
	AllocationProvider allocator;

	private OldBufferPacker() {
		//private
	}

	/** Does not retain packing list reference */
	public static ObjectArrayList<DrawableDelegate> pack(OldBufferPackingList packingList, OldVertexCollectorList collectorList, AllocationProvider allocator) {
		final OldBufferPacker packer = THREADLOCAL.get();
		final ObjectArrayList<DrawableDelegate> result = DelegateLists.getReadyDelegateList();
		packer.delegates = result;
		packer.collectorList = collectorList;
		packer.allocator = allocator;
		packingList.forEach(packer);
		packer.delegates = null;
		packer.collectorList = null;
		packer.allocator = null;
		return result;
	}

	public void accept(OldMaterialState materialState, int vertexStart, int vertexCount) {
		final OldVertexCollector collector = collectorList.get(materialState);
		final OldMaterialVertexFormat format = collector.format();
		final int stride = format.vertexStrideBytes;
		allocator.claimAllocation(vertexCount * stride, ref -> {
			final int byteOffset = ref.byteOffset();
			final int byteCount = ref.byteCount();
			final int intLength = byteCount / 4;

			ref.buffer().lockForWrite();
			final IntBuffer intBuffer = ref.intBuffer();
			intBuffer.position(byteOffset / 4);
			intBuffer.put(collector.rawData(), vertexStart * stride / 4, intLength);
			ref.buffer().unlockForWrite();

			delegates.add(DrawableDelegate.claim(ref, materialState, byteCount / stride, format));
		});
	}
}